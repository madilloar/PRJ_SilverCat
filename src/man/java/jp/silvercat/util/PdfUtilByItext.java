package jp.silvercat.util;

import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfUtilByItext {
  /**
   * Manipulates a PDF file src with the file dest as result
   * 
   * @param src
   *          the original PDF
   * @param dest
   *          the resulting PDF
   * @throws IOException
   * @throws DocumentException
   */
  public void decryptPdf(String src, String dest, byte[] ownerPassword) throws IOException, DocumentException {
    PdfReader reader = new PdfReader(src, ownerPassword);
    PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
    stamper.close();
    reader.close();
  }

  /**
   * Manipulates a PDF file src with the file dest as result
   * 
   * @param src
   *          the original PDF
   * @param dest
   *          the resulting PDF
   * @throws IOException
   * @throws DocumentException
   */
  public void encryptPdf(String src, String dest, byte[] userPassword, byte[] ownerPassword) throws IOException, DocumentException {
    PdfReader reader = new PdfReader(src);
    PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
    stamper.setEncryption(userPassword, ownerPassword, PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128
        | PdfWriter.DO_NOT_ENCRYPT_METADATA);
    stamper.close();
    reader.close();
  }

}
