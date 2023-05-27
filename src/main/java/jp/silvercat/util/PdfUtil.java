package jp.silvercat.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.silvercat.model.PdfPageModel;

public class PdfUtil implements IModel {
	/**
	 * Log instance.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PdfUtil.class);

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
	 * @param pages PDFページモデルのリスト。
	 * @param outputPdfFile マージした結果のPDFファイル。
	 * @throws IOException
	 */
	public void mergePdfPageModelToPdfFile(List<PdfPageModel> pages, File outputPdfFile) throws IOException {
		this.status = STATUS_CODE.MERGE_PDF_FILE_START;

		PDDocument doc = this.parseDocument(pages.get(0).getPdfFile());

		for (int i = 1; i < pages.size(); i++) {
			PDDocument pageDoc = this.parseDocument(pages.get(i).getPdfFile());
			doc.importPage((PDPage) pageDoc.getDocumentCatalog().getPages().get(0));
		}
		try {
			doc.save(outputPdfFile);
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
	public PDDocument setUserPassword(PDDocument orgDocument, String ownerPassword, String userPassword)
			throws IOException {
		StandardProtectionPolicy pol = new StandardProtectionPolicy(ownerPassword, userPassword,
				AccessPermission.getOwnerAccessPermission());
		orgDocument.protect(pol);
		return orgDocument;
	}

	/**
	 * 文書に制限をかけます。 勉強用に作成。印刷のみOKとする制限をかけます。暗号化キー長は128bitです。
	 * StandardProtectionPolicyの第2引数は文書を開くパスワード(USER PASSWORD)ですが、
	 * このメソッドでは""の空文字列としています。
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
		ap.setCanPrintFaithful(true);
		StandardProtectionPolicy pol = new StandardProtectionPolicy(ownerPassowrd, "", ap);
		pol.setEncryptionKeyLength(128);
		orgDocument.protect(pol);
		return orgDocument;
	}

	/**
	 * PDFファイルで使用しているフォントの一覧。 勉強用。
	 *
	 * @param orgDocument
	 * @throws IOException
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private void printDocumentFonts(PDDocument orgDocument) throws IOException {
		int i = 0;
		for (PDPage page : (List<PDPage>) orgDocument.getDocumentCatalog().getPages()) {
			i++;
			Iterable<COSName> pageFonts = page.getResources().getFontNames();
			for (COSName cosName : pageFonts) {
				LOG.debug("pageNum(" + i + "),pageFont key:" + cosName.getName());
				PDFont font = page.getResources().getFont(cosName);
				LOG.debug(",font:" + font.toString());
			}
		}
	}

	/**
	 * selectPageNumberで指定されたページをoutputDirで指定されたディレクトリにPDFファイルとして作成します。
	 * その時、rotation(0,90,180,270のいずれか)で指定された角度でページを回転してからPDFファイルを作成します。
	 *  問題:
	 * PDFファイルにフォントが埋め込んであり、 且つ、このプログラムを利用しているマシンにそのフォントがインストールされていない、 
	 * 且つ、OWNER PASSWORD(権限パスワード)により、「ページの抽出:許可しない」などの文書制限がかかっていると、
	 * ページを切り出して新たにPDFファイルを作ると、新たにできたPDFファイルでそのフォントを失う。
	 *
	 * @param inputPdfFile
	 * @param selectPageNumber
	 * @param outputDir
	 * @param rotation
	 * @return 作成されたPDFファイルのFileオブジェクト。
	 * @throws IOException
	 */
	public File createPdfThisPage(File inputPdfFile, int selectPageNumber, File outputDir, int rotation)
			throws IOException {
		this.status = STATUS_CODE.CREATE_PDF_THIS_PAGE_NUMBER_START;
		File outputPdfFile = null;
		PDDocument orgDocument = null;
		PDDocument newDocument = null;
		try {
			// iTextのOWNERパスワード解除を使う。オーナーパスワードは""で試している。
			orgDocument = this.parseDocument(inputPdfFile);
			newDocument = new PDDocument();

			PDPage newPage = (PDPage) orgDocument.getDocumentCatalog().getPages().get(selectPageNumber - 1);
			// ページの回転。
			newPage.setRotation(rotation);
			newDocument.importPage(newPage);

			String orgFileName = inputPdfFile.getName();
			String fileName = outputDir.toString() + "/" + this.getPreffix(orgFileName) + "-" + selectPageNumber
					+ ".pdf";

			outputPdfFile = new File(fileName);
			newDocument.save(outputPdfFile);
		} finally {
			if (newDocument != null) {
				newDocument.close();
			}
			if (orgDocument != null) {
				orgDocument.close();
			}
		}
		this.status = STATUS_CODE.CREATE_PDF_THIS_PAGE_NUMBER_END;
		this.notifyListeners();

		return outputPdfFile;
	}

	/**
	 * PDFファイルを読み、PDFページモデルのListとして返します。 PDFページモデルにはページのサムネイル画像を保持します。
	 * 時間のかかる処理なので、1ページ処理するごとにリスナーに通知します。
	 * サムネイル画像の画像タイプは、BufferedImage.TYPE_BYTE_INDEXEDで作ります。
	 *
	 * @param inputPdfFile PDFファイルのFileオブジェクト。
	 * @return PDFページモデルのListとして返します。
	 */
	public List<PdfPageModel> loadPdf(File inputPdfFile) {
		return loadPdf(inputPdfFile, BufferedImage.TYPE_BYTE_INDEXED);
	}

	/**
	 * PDFファイルを読み、PDFページモデルのListとして返します。 PDFページモデルにはページのサムネイル画像を保持します。
	 * 時間のかかる処理なので、1ページ処理するごとにリスナーに通知します。
	 *
	 * @param inputPdfFile PDFファイルのFileオブジェクト。
	 * @param imageType サムネイル画像の画像タイプ。BufferedImage.TYPE_INT_RGBとか。
	 * @return PDFページモデルのListとして返します。
	 */
	public List<PdfPageModel> loadPdf(File inputPdfFile, int imageType) {
		this.status = STATUS_CODE.LOAD_PDF_START;

		List<PdfPageModel> list = new CopyOnWriteArrayList<PdfPageModel>();
		PDDocument document = null;
		try {
			document = this.parseDocument(inputPdfFile);

			PDFRenderer pdfRenderer = new PDFRenderer(document);
			PDPageTree pageTree = document.getDocumentCatalog().getPages();
			int pageCounter = 0;
			for (PDPage page : pageTree) {
				pageCounter++;
				// renderImageWithDPIの第一引数はゼロベースインデックス
				BufferedImage img = pdfRenderer.renderImageWithDPI(pageCounter - 1, 300, ImageType.RGB);

				int w = (int) page.getCropBox().getWidth();
				int h = (int) page.getCropBox().getHeight();
				int r = page.getRotation();
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

				PdfPageModel pageModel = new PdfPageModel(inputPdfFile, pageCounter, thumbnail, r);

				list.add(pageModel);
				// Model Changed Notify
				this.processingPageNumber = pageCounter;
				this.notifyListeners();
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
		List<BufferedImage> list = new CopyOnWriteArrayList<BufferedImage>();
		PDDocument document = null;
		try {
			document = this.parseDocument(inputPdfFile);
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			PDPageTree pageTree = document.getDocumentCatalog().getPages();
			int pageCounter = 0;
			for (PDPage page : pageTree) {
				BufferedImage img = pdfRenderer.renderImageWithDPI(pageCounter++, 300, ImageType.RGB);
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
	 * PDFファイルを解析し、PDDocumentオブジェクトとして返します。文書を開くパスワードが施されていたら、USER PASSWORDを""で解除を試みます。
	 *
	 * @param inputPdfFile
	 * @return PDDocumentオブジェクト。
	 * @throws IOException
	 */
	public PDDocument parseDocument(File inputPdfFile) throws IOException {
		return this.parseDocument(inputPdfFile, "");
	}

	/**
	 * PDFファイルを解析し、PDDocumentオブジェクトとして返します。文書を開くパスワードが施されていたら、USER PASSWORDで解除を試みます。
	 *
	 * @param inputPdfFile
	 * @param userPassword PDFファイルを開くと、文書を開くパスワードダイアログが表示されるので、そのパスワード。
	 * @return PDDocumentオブジェクト。
	 * @throws IOException
	 */
	public PDDocument parseDocument(File inputPdfFile, String userPassword) throws IOException {
		PDDocument document = null;
		try {
			document = PDDocument.load(inputPdfFile);
			if (document.isEncrypted()) {
				// 文書を開くパスワードが施されていなくとも、OWNER PASSWORDを用いてPDFのセキュリティ(文書のコピーを許可しないなど)が設定されている場合があります。
				// この場合、ここでsetAllSecurityToBeRemoved(true)をしておかないと、この後の処理で、
				// java.lang.IllegalStateException: PDF contains an encryption dictionary, please remove it with setAllSecurityToBeRemoved() or set a protection policy with protect()
				// の例外となる。
				document.setAllSecurityToBeRemoved(true);
			}
		} catch (InvalidPasswordException e) {
			byte[] decrypt = this.decryptPdf(inputPdfFile, userPassword);
			document = PDDocument.load(new ByteArrayInputStream(decrypt));
		}
		return document;

	}

	/**
	 * PDFファイルに文書を開くパスワードが設定されているか？
	 * @param inputPdfFile
	 * @param userPassword
	 * @return
	 * @throws IOException
	 */
	public boolean verifyUserPassword(File inputPdfFile, String userPassword) throws IOException {
		try (PDDocument document = PDDocument.load(inputPdfFile, userPassword)) {
			return true;
		} catch (InvalidPasswordException e) {
			return false;
		}
	}

	/**
	 * PDFファイルのUSER PASSWORDを解除します。
	 *
	 * @param inputPdfFile
	 * @param userPassword
	 * @return
	 * @throws IOException
	 */
	byte[] decryptPdf(File inputPdfFile, String userPassword) throws IOException {
		ByteArrayOutputStream baos = null;
		try (PDDocument document = PDDocument.load(inputPdfFile, userPassword)) {
			if (document.isEncrypted()) {
				// ここでsetAllSecurityToBeRemoved(true)をしておかないと、この後のsaveメソッドで
				// java.lang.IllegalStateException: PDF contains an encryption dictionary, please remove it with setAllSecurityToBeRemoved() or set a protection policy with protect()
				// の例外となる。
				document.setAllSecurityToBeRemoved(true);
				baos = new ByteArrayOutputStream();
				document.save(baos);
			}
		}
		return baos.toByteArray();
	}

	/**
	 * ファイル名から拡張子を取り除いた名前を返します。
	 *
	 * @param fileName ファイル名
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

	//
	// IModelインターフェースの実装
	//
	/**
	 * 処理状況を監視するリスナーを保存。
	 */
	private List<IModelListener> listeners = new CopyOnWriteArrayList<IModelListener>();
	/**
	 * 処理状況を通知するためのイベントオブジェクト。
	 */
	private ModelEvent event = null;

	/**
	 * ステータスコードのenum。
	 */
	public enum STATUS_CODE implements IStatusCode {
		LOAD_PDF_START, LOAD_PDF_END, CREATE_PDF_THIS_PAGE_NUMBER_START, CREATE_PDF_THIS_PAGE_NUMBER_END, MERGE_PDF_FILE_START, MERGE_PDF_FILE_END, END;
	}

	/**
	 * ステータス。
	 */
	private IStatusCode status = null;

	/**
	 * リスナーを登録します。
	 */
	@Override
	public void addModelListener(IModelListener l) {
		this.listeners.add(l);
	}

	/**
	 * リスナーに変更を通知します。
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

	/**
	 * ステータスコード取得。
	 *
	 * @return ステータスコード。
	 */
	@Override
	public IStatusCode getStatus() {
		return this.status;
	}

	/**
	 * リスナーを解除します。
	 */
	@Override
	public void removeModelListner(IModelListener l) {
		this.listeners.remove(l);
	}

	/**
	 * クローズ処理。
	 */
	@Override
	public void close() {
		this.status = STATUS_CODE.END;
		this.notifyListeners();
	}
}
