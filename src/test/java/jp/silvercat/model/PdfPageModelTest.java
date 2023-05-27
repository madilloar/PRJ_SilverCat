/**
 * 
 */
package jp.silvercat.model;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class PdfPageModelTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testHashCode() throws IOException {
		File pdfFile = new File("./src/test/resources/NO1.pdf");
		Image img = this.createDummyImage();
		PdfPageModel exp = new PdfPageModel(pdfFile, 3, img, 0, "");
		PdfPageModel act = new PdfPageModel(pdfFile, 3, img, 0, "");
		assertEquals(exp.hashCode(), act.hashCode());
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEqualsObject() throws IOException {
		File pdfFile = new File("./src/test/resources/NO1.pdf");
		Image img = this.createDummyImage();
		PdfPageModel exp = new PdfPageModel(pdfFile, 3, img, 0, "");
		PdfPageModel act = new PdfPageModel(pdfFile, 3, img, 0, "");
		assertTrue(exp.equals(act));
		assertFalse(exp.equals(new String("")));
	}

	@Test
	public void testToString() {
		File pdfFile = new File("./src/test/resources/NO1.pdf");
		Image img = this.createDummyImage();
		PdfPageModel exp = new PdfPageModel(pdfFile, 3, img, 0, "");
		PdfPageModel act = new PdfPageModel(pdfFile, 3, img, 0, "");
		assertEquals(exp.toString(), act.toString());
	}

	@Test
	public void testRotate() {
		File pdfFile = new File("./src/test/resources/NO1.pdf");
		Image img = this.createDummyImage();
		PdfPageModel exp = new PdfPageModel(pdfFile, 3, img, 0, "");
		PdfPageModel act = new PdfPageModel(pdfFile, 3, img, 0, "");
		exp.rotate();
		act.rotate();
		byte[] bExp = this.convertBufferedImageToJpegByteArray(
				this.convertImageToBufferedImage(exp.getPdfPageImage().getImage()));
		byte[] bAct = this.convertBufferedImageToJpegByteArray(
				this.convertImageToBufferedImage(act.getPdfPageImage().getImage()));
		assertArrayEquals(bExp, bAct);
	}

	private byte[] convertBufferedImageToJpegByteArray(BufferedImage image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "jpg", baos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte[] bytes = baos.toByteArray();
		return bytes;
	}

	private BufferedImage convertImageToBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

	/**
	 * PdfPageModelの引数にImageが必要なので、とりあえず、ダミーのImageを作る。
	 * 
	 * @return 800x600 HelloWorldのイメージ。
	 */
	private Image createDummyImage() {
		int w = 800, h = 600;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, w, h);
		g2d.setColor(Color.BLACK);
		g2d.drawString("HelloWorld", 50, 50);
		try {
			ImageIO.write(image, "JPEG", new File("./src/test/resources/temp.jpg"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return image;
	}

}
