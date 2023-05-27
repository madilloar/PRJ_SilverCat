package jp.silvercat.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.silvercat.util.IModel;
import jp.silvercat.util.IModelListener;
import jp.silvercat.util.IStatusCode;
import jp.silvercat.util.ModelEvent;
import jp.silvercat.util.PdfUtil;

public class PdfFileModel implements IModel, Cloneable, Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(PdfFileModel.class);

  private File pdfFile = null;
  private int totalPageNumber = 0;
  private int processingPageNumber = 0;
  private List<PdfPageModel> pages = null;
  private PdfFileModel self_ = null;

  public PdfFileModel() {
    this.self_ = this;
  }

  public PdfFileModel(File newPdfFile) {
    this();
    this.pdfFile = newPdfFile;
    this.pages = new CopyOnWriteArrayList<PdfPageModel>();
  }

  public int getProcessingPageNumber() {
    return processingPageNumber;
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
    this.notifyListeners();
  }

  @SuppressWarnings("unchecked")
  public void addPage(List<?> pages) {
    this.status = STATUS_CODE.ADD_PAGE_PROCESSING;
    this.pages.addAll((List<PdfPageModel>) pages);
    this.totalPageNumber = pages.size();
    this.processingPageNumber = this.totalPageNumber;
    this.status = STATUS_CODE.ADD_PAGE_PROCESSEND;
    this.notifyListeners();
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
        this.notifyListeners();
      }
      this.status = STATUS_CODE.TEMP_PDF_PAGE_PROCESSEND;
      this.notifyListeners();

      // PDFファイルをマージする
      this.status = STATUS_CODE.CREATE_PDF_FILE_PROCESSING;
      PdfUtil u = new PdfUtil();
      u.mergePdfPageModelToPdfFile(pages, this.pdfFile);
      this.status = STATUS_CODE.CREATE_PDF_FILE_PROCESSEND;
      this.notifyListeners();
    } finally {
      ;
    }

  }

  public void load(File file) {
    this.status = STATUS_CODE.LOADING;
    this.pdfFile = file;

    // PdfUtilの画像処理に時間がかかるので、進捗バー用のリスナーを用意
    IModelListener ml = new IModelListener() {
      @Override
      public void modelChanged(ModelEvent event) {
        PdfUtil model = (PdfUtil) event.getSource();
        if (model.getStatus().equals(PdfUtil.STATUS_CODE.LOAD_PDF_START)) {
          self_.totalPageNumber = model.getProcessDocumentPagesSize();
          self_.processingPageNumber = model.getProcessingPageNumber();
          self_.notifyListeners();
          return;
        }
        if (model.getStatus().equals(PdfUtil.STATUS_CODE.LOAD_PDF_END)) {
          model.removeModelListner(this);
        }
      }
    };
    PdfUtil u = new PdfUtil();
    u.addModelListener(ml);
    List<PdfPageModel> pageModels = u.loadPdf(file);

    this.totalPageNumber = pageModels.size();
    this.pages = pageModels;

    // Modelの状態が変化したのでリスナーに通知する。
    this.status = STATUS_CODE.LOADEND;
    this.notifyListeners();
  }

  public List<PdfPageModel> getPages() {
    return pages;
  }

  public Object[] getRow() {
    return new Object[] { this.pdfFile.getName(), this.totalPageNumber, this };
  }

  @Override
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

  @Override
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

  @Override
  public String toString() {
    return this.pdfFile.toString() + "," + this.totalPageNumber;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  //
  // IModelインターフェースの実装
  //
  // スレッドセーフなリストなので、addとremoveが頻繁にあると、遅くなるが、
  // 検索は速いListとのこと。
  private List<IModelListener> listeners = new CopyOnWriteArrayList<IModelListener>();
  private ModelEvent event = null;
  private IStatusCode status = null;

  public enum STATUS_CODE implements IStatusCode {
    LOADING, LOADEND, ADD_PAGE_PROCESSING, ADD_PAGE_PROCESSEND, TEMP_PDF_PAGE_PROCESSING, TEMP_PDF_PAGE_PROCESSEND, CREATE_PDF_FILE_PROCESSING, CREATE_PDF_FILE_PROCESSEND, END;
  }

  @Override
  public void addModelListener(IModelListener l) {
    this.listeners.add(l);
  }

  /**
   * リスナー達に状態変化を通知する。
   */
  @Override
  public void notifyListeners() {
    for (IModelListener listener : listeners) {
      if (this.event == null) {
        this.event = new ModelEvent(this);
      }
      listener.modelChanged(this.event);
    }
  }

  @Override
  public IStatusCode getStatus() {
    return status;
  }

  @Override
  public void removeModelListner(IModelListener l) {
    this.listeners.remove(l);
  }

  @Override
  public void close() {
    LOG.debug(this.getClass().getName() + "#close()");

    for (PdfPageModel page : pages) {
      page.close();
    }
    this.status = STATUS_CODE.END;
    this.notifyListeners();
  }

}
