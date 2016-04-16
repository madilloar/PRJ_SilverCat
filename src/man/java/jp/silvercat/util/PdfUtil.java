package jp.silvercat.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.EventListenerList;

import jp.silvercat.model.PdfPageModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

public class PdfUtil implements IModel {
  /**
   * Log instance.
   */
  private static final Log LOG = LogFactory.getLog(PdfUtil.class);

  /**
   * ステータスコードのenum。
   */
  public enum STATUS_CODE {
    LOAD_PDF_START, LOAD_PDF_END, CREATE_PDF_THIS_PAGE_NUMBER_START, CREATE_PDF_THIS_PAGE_NUMBER_END, MERGE_PDF_FILE_START, MERGE_PDF_FILE_END
  }

  /**
   * 処理状況を監視するリスナーを保存。
   */
  private EventListenerList listeners = new EventListenerList();
  /**
   * 処理状況を通知するためのイベントオブジェクト。
   */
  private ModelEvent event = null;
  /**
   * ステータス。
   */
  private STATUS_CODE status = null;
  // 進捗状況通知用変数
  /**
   * 現在処理しているPDFファイルの全ページ数。
   */
  private int processDocumentPagesSize = 0;
  /**
   * 現在処理しているPDFファイルのページ番号。
   */
  private int processingPageNumber = 0;

  /**
   * コンストラクタ。
   */
  public PdfUtil() {

  }

  /**
   * ステータスコード取得。
   * 
   * @return ステータスコード。
   */
  public STATUS_CODE getStatus() {
    return this.status;
  }

  /**
   * 現在処理しているPDFファイルの全ページ数。
   * 
   * @return 現在処理しているPDFファイルの全ページ数。
   */
  public int getProcessDocumentPagesSize() {
    return processDocumentPagesSize;
  }

  /**
   * 現在処理しているPDFファイルのページ番号。
   * 
   * @return 現在処理しているPDFファイルのページ番号。
   */
  public int getProcessingPageNumber() {
    return processingPageNumber;
  }

  /**
   * PDFページモデルをマージします。 PDFページモデルのリストをマージして、outputPdfFileで指定されたPDFファイルに出力します。
   * PDFページモデルは、PDFファイルのとあるページが、1つのPDFファイルとなっているものです。
   * 
   * @param pages
   *          PDFページモデルのリスト。
   * @param outputPdfFile
   *          マージした結果のPDFファイル。
   * @throws IOException
   */
  public void mergePdfPageModelToPdfFile(List<PdfPageModel> pages, File outputPdfFile) throws IOException {
    this.status = STATUS_CODE.MERGE_PDF_FILE_START;

    PDDocument doc = this.parseDocument(pages.get(0).getPdfFile());

    for (int i = 1; i < pages.size(); i++) {
      PDDocument pageDoc = this.parseDocument(pages.get(i).getPdfFile());
      doc.importPage((PDPage) pageDoc.getDocumentCatalog().getAllPages().get(0));
    }
    try {
      doc.save(outputPdfFile);
    } catch (COSVisitorException e) {
      throw new IOException(e);
    } finally {
      doc.close();
    }

    this.status = STATUS_CODE.MERGE_PDF_FILE_END;
  }

  /**
   * PDFファイルにUSER PASSWORDを設定します。 勉強用に作成したメソッド。USER PASSWORDが設定されたPDFファイルを保存し、
   * それを開くと、そのUSER PASSWORDパスワードが要求されるようになります。
   * 
   * @param orgDocument
   * @param userPassword
   * @return USER PASSWORDが設定されたPDDocument。
   * @throws IOException
   */
  public PDDocument setUserPassword(PDDocument orgDocument, String ownerPassword, String userPassword) throws IOException {
    StandardProtectionPolicy pol = new StandardProtectionPolicy(ownerPassword, userPassword,
        AccessPermission.getOwnerAccessPermission());
    try {
      orgDocument.protect(pol);
    } catch (BadSecurityHandlerException e) {
      throw new IOException(e);
    }
    return orgDocument;
  }

  /**
   * 文書に制限をかけます。 勉強用に作成。印刷のみOKとする制限をかけます。暗号化キー長は128bitです。 文書制限には、OWNER
   * PASSWORDが必要ですが、このメソッドでは""の空文字列としています。
   * 
   * @param orgDocument
   * @return 印刷のみOKのPDDocument。
   * @throws IOException
   */
  public PDDocument setPermission(PDDocument orgDocument, String ownerPassowrd) throws IOException {
    AccessPermission ap = new AccessPermission();
    ap.setCanAssembleDocument(false);
    ap.setCanExtractContent(false);
    ap.setCanExtractForAccessibility(false);
    ap.setCanFillInForm(false);
    ap.setCanModify(false);
    ap.setCanModifyAnnotations(false);
    ap.setCanPrint(true);
    ap.setCanPrintDegraded(true);
    StandardProtectionPolicy pol = new StandardProtectionPolicy(ownerPassowrd, "", ap);
    pol.setEncryptionKeyLength(128);
    try {
      orgDocument.protect(pol);
    } catch (BadSecurityHandlerException e) {
      throw new IOException(e);
    }
    return orgDocument;
  }

  /**
   * PDFファイルで使用しているフォントの一覧。 勉強用。
   * 
   * @param orgDocument
   */
  @SuppressWarnings({ "unchecked", "unused" })
  private void printDocumentFonts(PDDocument orgDocument) {
    int i = 0;
    for (PDPage page : (List<PDPage>) orgDocument.getDocumentCatalog().getAllPages()) {
      i++;
      Map<String, PDFont> pageFonts = page.getResources().getFonts();
      Set<String> keys = pageFonts.keySet();
      for (String key : keys) {
        LOG.debug("pageNum(" + i + "),pageFont key:" + key);
        PDFont font = pageFonts.get(key);
        LOG.debug(",font:" + font.getBaseFont());
      }
    }
  }

  /**
   * selectPageNumberで指定されたページをoutputDirで指定されたディレクトリにPDFファイルとして作成します。
   * その時、rotation(0,90,180,270のいずれか)で指定された角度でページを回転してからPDFファイルを作成します。 問題:
   * PDFファイルにフォントが埋め込んであり、 且つ、このプログラムを利用しているマシンにそのフォントがインストールされていない、 且つ、OWNER
   * PASSWORD(権限パスワード)により、「ページの抽出:許可しない」などの文書制限がかかっていると、
   * ページを切り出して新たにPDFファイルを作ると、新たにできたPDFファイルでそのフォントを失う。 解決策: iTextのOWNER
   * PASSWORD解除を利用する。 (PDFBOXにはその機能がなさそう)
   * 
   * @param inputPdfFile
   * @param selectPageNumber
   * @param outputDir
   * @param rotation
   * @return 作成されたPDFファイルのFileオブジェクト。
   * @throws IOException
   */
  public File createPdfThisPage(File inputPdfFile, int selectPageNumber, File outputDir, int rotation) throws IOException {
    this.status = STATUS_CODE.CREATE_PDF_THIS_PAGE_NUMBER_START;
    File outputPdfFile = null;
    PDDocument orgDocument = null;
    PDDocument newDocument = null;
    try {
      // iTextのOWNERパスワード解除を使う。オーナーパスワードは””で試している。
      orgDocument = this.parseDocument(inputPdfFile);
      newDocument = new PDDocument();

      PDPage newPage = (PDPage) orgDocument.getDocumentCatalog().getAllPages().get(selectPageNumber - 1);
      // ページの回転。
      newPage.setRotation(rotation);
      newDocument.importPage(newPage);

      String orgFileName = inputPdfFile.getName();
      String fileName = outputDir.toString() + "/" + this.getPreffix(orgFileName) + "-" + selectPageNumber + ".pdf";

      outputPdfFile = new File(fileName);
      newDocument.save(outputPdfFile);
    } catch (COSVisitorException e) {
      throw new IOException(e);
    } finally {
      if (newDocument != null) {
        newDocument.close();
      }
      if (orgDocument != null) {
        orgDocument.close();
      }
    }
    this.status = STATUS_CODE.CREATE_PDF_THIS_PAGE_NUMBER_END;
    this.modelChanged();

    return outputPdfFile;
  }

  /**
   * PDFファイルを読み、PDFページモデルのListとして返します。 PDFページモデルにはページのサムネイル画像を保持します。
   * 時間のかかる処理なので、1ページ処理するごとにリスナーに通知します。
   * 
   * @param inputPdfFile
   *          PDFファイルのFileオブジェクト。
   * @param imageType
   *          サムネイル画像の画像タイプ。BufferedImage.TYPE_INT_RGBとか。
   * @return PDFページモデルのListとして返します。
   */
  public List<PdfPageModel> loadPdf(File inputPdfFile, int imageType) {
    this.status = STATUS_CODE.LOAD_PDF_START;

    List<PdfPageModel> list = new ArrayList<PdfPageModel>();
    PDDocument document = null;
    try {
      document = this.parseDocument(inputPdfFile);

      @SuppressWarnings("unchecked")
      List<PDPage> pages = document.getDocumentCatalog().getAllPages();
      processDocumentPagesSize = pages.size();
      for (int i = 0; i < processDocumentPagesSize; i++) {
        PDPage page = pages.get(i);
        BufferedImage img = page.convertToImage();
        int w = (int) page.findCropBox().getWidth();
        int h = (int) page.findCropBox().getHeight();
        int r = page.findRotation();
        LOG.debug("w:" + w + ",h:" + h + ",r:" + r);
        if (w > h || r == 90 || r == 270) {
          w = 141;
          h = 100;
        } else {
          w = 100;
          h = 141;
        }

        BufferedImage thumbnail = new BufferedImage(w, h, imageType);
        thumbnail.getGraphics().drawImage(img, 0, 0, w, h, null);

        PdfPageModel pageModel = new PdfPageModel(inputPdfFile, i + 1, thumbnail, r);

        list.add(pageModel);
        // Model Changed Notify
        this.processingPageNumber = i;
        this.modelChanged();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (document != null) {
          document.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    this.status = STATUS_CODE.LOAD_PDF_END;
    return list;
  }

  /**
   * PDFファイルの各ページをイメージListとして返します。 勉強用に作成。
   * 
   * @param inputPdfFile
   * @param imageType
   * @return PDFファイルの各ページをイメージListとして返します。
   */
  public List<BufferedImage> pdfToImage(File inputPdfFile, int imageType) {
    List<BufferedImage> list = new ArrayList<BufferedImage>();
    PDDocument document = null;
    try {
      document = this.parseDocument(inputPdfFile);

      @SuppressWarnings("unchecked")
      List<PDPage> pages = document.getDocumentCatalog().getAllPages();
      processDocumentPagesSize = pages.size();
      for (int i = 0; i < processDocumentPagesSize; i++) {
        PDPage page = pages.get(i);
        BufferedImage img = page.convertToImage();
        list.add(img);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (document != null) {
          document.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return list;
  }

  /**
   * PDFファイルを解析し、PDDocumentオブジェクトとして返します。 暗号化されていたら、OWNER PASSWORDを""で解除を試みます。
   * 
   * @param inputPdfFile
   * @return PDDocumentオブジェクト。
   * @throws IOException
   */
  public PDDocument parseDocument(File inputPdfFile) throws IOException {
    // TODO
    // return this.parseDocument(inputPdfFile, "");
    return this.parseDocument(inputPdfFile, "ownerpassword");
  }

  /**
   * PDFファイルを解析し、PDDocumentオブジェクトとして返します。 暗号化されていたら、OWNER PASSWORDで解除を試みます。
   * 
   * @param inputPdfFile
   * @return PDDocumentオブジェクト。
   * @throws IOException
   */
  public PDDocument parseDocument(File inputPdfFile, String ownerPassword) throws IOException {
    PDDocument document = PDDocument.load(inputPdfFile);

    if (document.isEncrypted()) {
      document.close();
      byte[] decrypt = this.decryptPdf(inputPdfFile, ownerPassword.getBytes());
      document = PDDocument.load(new ByteArrayInputStream(decrypt));
    }

    return document;
  }

  /**
   * PDFファイルのOWNER PASSWORDを解除します。
   * 
   * @param inputPdfFile
   * @param ownerPassword
   * @return
   * @throws IOException
   */
  private byte[] decryptPdf(File inputPdfFile, byte[] ownerPassword) throws IOException {
    ByteArrayOutputStream dest = null;
    PdfReader reader = null;
    PdfStamper stamper = null;
    try {
      dest = new ByteArrayOutputStream();
      reader = new PdfReader(new FileInputStream(inputPdfFile), ownerPassword);
      stamper = new PdfStamper(reader, dest);
    } catch (IOException e) {
      throw e;
    } catch (DocumentException e) {
      throw new IOException(e);
    } finally {
      try {
        if (stamper != null) {
          stamper.close();
        }
      } catch (DocumentException e) {
        throw new IOException(e);
      }
      if (reader != null) {
        reader.close();
      }
    }
    return dest.toByteArray();
  }

  /**
   * ファイル名から拡張子を取り除いた名前を返します。
   * 
   * @param fileName
   *          ファイル名
   * @return ファイル名
   */
  private String getPreffix(String fileName) {
    if (fileName == null)
      return null;
    int point = fileName.lastIndexOf(".");
    if (point != -1) {
      return fileName.substring(0, point);
    }
    return fileName;
  }

  /**
   * リスナーに変更を通知します。
   */
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

  // IModelインターフェースの実装
  /**
   * リスナーを登録します。
   */
  public void addModelListener(IModelListener l) {
    this.listeners.add(IModelListener.class, l);
  }

  /**
   * リスナーを解除します。
   */
  public void removeModelListner(IModelListener l) {
    this.listeners.remove(IModelListener.class, l);
  }
}
