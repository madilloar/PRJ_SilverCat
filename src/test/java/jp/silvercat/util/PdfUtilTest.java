package jp.silvercat.util;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jp.silvercat.model.PdfPageModel;

public class PdfUtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test01() throws Exception {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/decrypted.pdf");
		File outDir = new File("./src/test/resources/SetPermission.pdf");
		PDDocument orgDocument = null;
		try {
			orgDocument = PDDocument.load(pdfFile);
			orgDocument = target.setPermission(orgDocument, "ownerpassword");
			orgDocument.save(outDir);
		} catch (Throwable e) {

			fail();
		} finally {
			if (orgDocument != null) {
				orgDocument.close();
			}
		}
	}

	@Test
	public void test02() throws Exception {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/decrypted.pdf");
		File outDir = new File("./src/test/resources/SetUserPassword.pdf");
		PDDocument orgDocument = null;
		try {
			orgDocument = PDDocument.load(pdfFile);
			orgDocument = target.setUserPassword(orgDocument, "ownerpassword", "password");
			orgDocument.save(outDir);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (orgDocument != null) {
				orgDocument.close();
			}
		}
	}

	@Test
	public void test03() throws Exception {
		PdfUtil target = new PdfUtil();
		File in = new File("./src/test/resources/SetPermission.pdf");
		File out = new File("./src/test/resources/output03.pdf");
		PDDocument orgDocument = null;
		try {
			orgDocument = target.parseDocument(in, "");
			orgDocument.save(out);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (orgDocument != null) {
				orgDocument.close();
			}
		}

	}

	@Test
	public void test04() throws Exception {
		PdfUtil target = new PdfUtil();
		PDDocument orgDocument = null;
		try {
			File pdfFile = new File("./src/test/resources/decrypted.pdf");
			orgDocument = target.parseDocument(pdfFile, "");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (orgDocument != null) {
				orgDocument.close();
			}
		}

		try {
			File pdfFile = new File("./src/test/resources/SetUserPassword.pdf");

			assertTrue(target.verifyUserPassword(pdfFile, "password"));

			orgDocument = target.parseDocument(pdfFile, "password");

			String actual = orgDocument.getDocumentInformation().getAuthor();
			assertEquals("lemac", actual);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (orgDocument != null) {
				orgDocument.close();
			}
		}
	}

	@Test
	public void test05() throws Exception {
		PdfUtil target = new PdfUtil();
		PDDocument orgDocument = null;
		try {
			File pdfFile = new File("./src/test/resources/SetPermission.pdf");
			orgDocument = target.parseDocument(pdfFile, "");
			orgDocument.save("./src/test/resources/output05.pdf");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (orgDocument != null) {
				orgDocument.close();
			}
		}
	}

	/**
	 * 期待値:encrypted-1.pdfの1ページ目が右に90度回転していること。
	 */
	@Test
	public void test06() {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/encrypted.pdf");
		File outDir = new File("./src/test/resources");
		int rotation = 90;
		try {
			// 1ページ目を切り出し、右に90度回転
			target.createPdfThisPage(pdfFile, 1, outDir, rotation, "");
		} catch (Throwable e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test07() {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/decrypted.pdf");
		File outDir = new File("./src/test/resources");
		int rotation = 90;
		try {
			target.createPdfThisPage(pdfFile, 2, outDir, rotation, "");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * 期待値:decrypted.pdfの3ページ目が右に90度回転してdecrpted03.pdfが保存されていること。
	 */
	@Test
	public void test08() {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/decrypted.pdf");
		File outDir = new File("./src/test/resources");
		int rotation = 90;
		try {
			target.createPdfThisPage(pdfFile, 3, outDir, rotation, "");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * 期待値:PDFのページを画像jpgにして保存する。
	 * @throws Exception
	 */
	@Test
	public void test09() throws Exception {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/encrypted.pdf");
		File out = new File("./src/test/resources/output09.jpg");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(out);
			List<BufferedImage> list = target.pdfToImage(pdfFile, BufferedImage.TYPE_INT_RGB);
			BufferedImage im = list.get(0);
			ImageIOUtil.writeImage(im, "jpg", fos);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	/**
	 * 期待値:PDFのページを画像jpgにして保存する。
	 * @throws Exception
	 */
	@Test
	public void test10() throws Exception {
		PdfUtil target = new PdfUtil();
		File pdfFile = new File("./src/test/resources/PDFA.pdf");
		File out = new File("./src/test/resources/output10.jpg");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(out);
			List<BufferedImage> list = target.pdfToImage(pdfFile, BufferedImage.TYPE_INT_RGB);
			BufferedImage im = list.get(0);
			ImageIOUtil.writeImage(im, "jpg", fos);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	/**
	 * 期待値:NO1.pdfとencrypted.pdfをマージしてoutput11.pdfにする。
	 */
	@Test
	public void test11() {
		PdfUtil target = new PdfUtil();
		try {
			List<PdfPageModel> pages = new ArrayList<PdfPageModel>();
			pages.add(new PdfPageModel(new File("./src/test/resources/NO1.pdf"), 1,
					new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY), 0, ""));
			pages.add(new PdfPageModel(new File("./src/test/resources/encrypted.pdf"), 1,
					new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY), 0, ""));
			target.mergePdfPageModelToPdfFile(pages, new File("./src/test/resources/output11.pdf"), "");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
