package jp.silvercat.util;

import static org.junit.Assert.fail;

import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfUtilByItextTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  /** User password. */
  public static byte[] USER_PASSWOARD = "Hello".getBytes();
  /** Owner password. */
  public static byte[] OWNER_PASSWOARD = "World".getBytes();

  /** The resulting PDF file. */
  public static final String RESULT1 = "./src/test/recources/encryption.pdf";
  /** The resulting PDF file. */
  public static final String RESULT2 = "./src/test/recources/encryption_decrypted.pdf";
  /** The resulting PDF file. */
  public static final String RESULT3 = "./src/test/recources/encryption_encrypted.pdf";

  @Before
  public void setUp() throws Exception {
    // step 1
    Document document = new Document();
    // step 2
    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(RESULT1));
    writer.setEncryption(USER_PASSWOARD, OWNER_PASSWOARD, PdfWriter.ALLOW_PRINTING, PdfWriter.STANDARD_ENCRYPTION_128);
    writer.createXmpMetadata();
    // step 3
    document.open();
    // step 4
    document.add(new Paragraph("Hello World"));
    // step 5
    document.close();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testDecryptPdf() {
    PdfUtilByItext metadata = new PdfUtilByItext();
    try {
      metadata.decryptPdf(RESULT1, RESULT2, OWNER_PASSWOARD);
    } catch (IOException | DocumentException e) {
      e.printStackTrace();

    }
  }

  @Test
  public void testDecryptPdf2() {
    PdfUtilByItext metadata = new PdfUtilByItext();
    try {
      String org = "./src/test/recources/SetPermission.pdf";
      metadata.decryptPdf(org, "./src/test/recources/output1.pdf", "ownerpassword".getBytes());
    } catch (IOException | DocumentException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testEncryptPdf() {
    PdfUtilByItext metadata = new PdfUtilByItext();
    try {
      metadata.encryptPdf(RESULT2, RESULT3, USER_PASSWOARD, OWNER_PASSWOARD);
    } catch (IOException | DocumentException e) {
      e.printStackTrace();
      fail();
    }
  }

}
