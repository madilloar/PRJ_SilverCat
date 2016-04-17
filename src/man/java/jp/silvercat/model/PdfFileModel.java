package jp.silvercat.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;

import jp.silvercat.util.IModel;
import jp.silvercat.util.IModelListener;
import jp.silvercat.util.ModelEvent;
import jp.silvercat.util.PdfUtil;

@SuppressWarnings("serial")
public class PdfFileModel implements IModel, Cloneable, Serializable {
  public enum STATUS_CODE {
    LOADING, LOADEND, ADD_PAGE_PROCESSING, ADD_PAGE_PROCESSEND, TEMP_PDF_PAGE_PROCESSING, TEMP_PDF_PAGE_PROCESSEND, CREATE_PDF_FILE_PROCESSING, CREATE_PDF_FILE_PROCESSEND
  }

  private EventListenerList listeners = new EventListenerList();
  ModelEvent event = null;

  private File pdfFile = null;
  private int totalPageNumber = 0;
  private int processingPageNumber = 0;
  private List<PdfPageModel> pages = null;
  private STATUS_CODE status = null;
  private PdfFileModel self_ = null;

  public PdfFileModel() {
    this.self_ = this;
  }

  public PdfFileModel(File newPdfFile) {
    this();
    this.pdfFile = newPdfFile;
    this.pages = new ArrayList<PdfPageModel>();
  }

  public int getProcessingPageNumber() {
    return processingPageNumber;
  }

  public STATUS_CODE getStatus() {
    return status;
  }

  public int getTotalPageNumber() {
    return totalPageNumber;
  }

  public void addPage(PdfPageModel page) {
    this.status = STATUS_CODE.ADD_PAGE_PROCESSING;
    this.pages.add(page);
    this.totalPageNumber++;
    this.processingPageNumber = this.totalPageNumber;
    this.status = STATUS_CODE.ADD_PAGE_PROCESSEND;
    this.modelChanged();
  }

  @SuppressWarnings("unchecked")
  public void addPage(List<?> pages) {
    this.status = STATUS_CODE.ADD_PAGE_PROCESSING;
    this.pages.addAll((List<PdfPageModel>) pages);
    this.totalPageNumber = pages.size();
    this.processingPageNumber = this.totalPageNumber;
    this.status = STATUS_CODE.ADD_PAGE_PROCESSEND;
    this.modelChanged();
  }

  public void createPdfFile() throws IOException {
    Path outputDir = null;
    try {
      outputDir = Files.createTempDirectory(null);
      // 各ページをtempディレクトリいにPDFファイルとして出力する
      for (int i = 0; i < this.pages.size(); i++) {
        this.status = STATUS_CODE.TEMP_PDF_PAGE_PROCESSING;
        PdfPageModel page = this.pages.get(i);
        page.createTempPdfThisPage(outputDir.toFile());
        this.processingPageNumber = i;
        // 通知
        this.modelChanged();
      }
      this.status = STATUS_CODE.TEMP_PDF_PAGE_PROCESSEND;
      this.modelChanged();

      // PDFファイルをマージする
      this.status = STATUS_CODE.CREATE_PDF_FILE_PROCESSING;
      PdfUtil u = new PdfUtil();
      u.mergePdfPageModelToPdfFile(pages, this.pdfFile);
      this.status = STATUS_CODE.CREATE_PDF_FILE_PROCESSEND;
      this.modelChanged();
    } finally {
      ;
    }

  }

  public void load(File file) {
    this.status = STATUS_CODE.LOADING;
    this.pdfFile = file;

    // PdfUtilの画像処理に時間がかかるので、進捗バー用のリスナーを用意
    IModelListener ml = new IModelListener() {
      public void modelChanged(ModelEvent event) {
        PdfUtil model = (PdfUtil) event.getSource();
        if (model.getStatus().equals(PdfUtil.STATUS_CODE.LOAD_PDF_START)) {
          self_.totalPageNumber = model.getProcessDocumentPagesSize();
          self_.processingPageNumber = model.getProcessingPageNumber();
          self_.modelChanged();
          return;
        }
        if (model.getStatus().equals(PdfUtil.STATUS_CODE.LOAD_PDF_END)) {
          model.removeModelListner(this);
        }
      }
    };
    PdfUtil u = new PdfUtil();
    u.addModelListener(ml);
    List<PdfPageModel> pageModels = u.loadPdf(file, BufferedImage.TYPE_BYTE_INDEXED);

    this.totalPageNumber = pageModels.size();
    this.pages = pageModels;

    // Modelの状態が変化したのでリスナーに通知する。
    this.status = STATUS_CODE.LOADEND;
    this.modelChanged();
  }

  public List<PdfPageModel> getPages() {
    return pages;
  }

  public Object[] getRow() {
    return new Object[] { this.pdfFile.getName(), this.totalPageNumber, this };
  }

  protected void modelChanged() {

    // Guaranteed to return a non-null array
    Object[] listeners = this.listeners.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IModelListener.class) {
        // Lazily create the event:
        if (event == null)
          event = new ModelEvent(this);
        ((IModelListener) listeners[i + 1]).modelChanged(event);
      }
    }
  }

  public PdfPageModel clone() {
    try {
      return (PdfPageModel) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public PdfPageModel deepClone() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      return (PdfPageModel) ois.readObject();
    } catch (IOException e) {
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public boolean equals(Object anObject) {
    if (this == anObject) {
      return true;
    }
    if (anObject instanceof PdfPageModel) {
      PdfPageModel anotherString = (PdfPageModel) anObject;
      return (this.toString().equals(anotherString.toString()));
    }
    return false;
  }

  public int compareTo(PdfPageModel another) {
    return this.toString().compareTo(another.toString());
  }

  public String toString() {
    return this.pdfFile.toString() + "," + this.totalPageNumber;
  }

  public int hashCode() {
    return toString().hashCode();
  }

  // IModelインターフェースの実装
  public void addModelListener(IModelListener l) {
    this.listeners.add(IModelListener.class, l);
  }

  public void removeModelListner(IModelListener l) {
    this.listeners.remove(IModelListener.class, l);
  }

}
