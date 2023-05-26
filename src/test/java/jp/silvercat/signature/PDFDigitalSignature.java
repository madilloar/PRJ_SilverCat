package jp.silvercat.signature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/**
 * 
 * java\11\bin\keytool -genkeypair -alias mycert -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -dname "cn=madilloar, ou=mydep, o=mycorp, l=Tokyo, st=Chiyoda-ku, c=JP" -validity 3560 -storetype PKCS12 -keystore C:\Users\lemac\keystore.p12 -storepass password!
 * 
 * @author madilloar.
 *
 */
public class PDFDigitalSignature {
	class DigitalSignatureUtil implements SignatureInterface {
		PrivateKey privateKey_ = null;
		Certificate[] certificateChain_ = null;

		public DigitalSignatureUtil() {
			super();
		}

		public DigitalSignatureUtil(PrivateKey privateKey, Certificate[] certificateChain) {
			privateKey_ = privateKey;
			certificateChain_ = certificateChain;
		}

		/**
		 * contentは呼び出し側でcloseする。
		 */
		@Override
		public byte[] sign(InputStream content) throws IOException {
			try {
				CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
				X509Certificate cert = (X509Certificate) certificateChain_[0];
				ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey_);
				gen.addSignerInfoGenerator(
						new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
								.build(sha1Signer, cert));
				gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain_)));
				CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
				CMSSignedData signedData = gen.generate(msg, false);
				return signedData.getEncoded();
			} catch (GeneralSecurityException e) {
				throw new IOException(e);
			} catch (CMSException e) {
				throw new IOException(e);
			} catch (OperatorCreationException e) {
				throw new IOException(e);
			}
		}

	}

	class CMSProcessableInputStream implements CMSTypedData {
		private InputStream in;
		private final ASN1ObjectIdentifier contentType;

		CMSProcessableInputStream(InputStream is) {
			this(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), is);
		}

		CMSProcessableInputStream(ASN1ObjectIdentifier type, InputStream is) {
			contentType = type;
			in = is;
		}

		@Override
		public Object getContent() {
			return in;
		}

		@Override
		public void write(OutputStream out) throws IOException, CMSException {
			// read the content only one time
			IOUtils.copy(in, out);
			in.close();
		}

		@Override
		public ASN1ObjectIdentifier getContentType() {
			return contentType;
		}
	}

	public static void main(String[] args) throws Exception {
		PDFDigitalSignature me = new PDFDigitalSignature();
		me.execute();
	}

	private void execute() throws Exception {
		// Signer signer = this.createSigner(new File("./docs/keystore.p12"), "mycert", "password!");
		DigitalSignatureUtil signer = this.createSigner();

		FileOutputStream fos = new FileOutputStream("./docs/signed.pdf");
		try (InputStream is = new FileInputStream("./docs/memo.pdf")) {
			PDDocument doc = PDDocument.load(is);
			this.signDetached(doc, fos, signer);
			doc.close();
			System.out.println("PDFファイルに電子署名をしました。");
		}
	}

	/**
	 * 電子署名ユーティリティオブジェクト作成.
	 * 
	 * 電子署名利用する公開鍵＋秘密鍵のペアと電子証明書はファイルパスに指定されたPKCS12形式のキーストアファイルから読み込みます。
	 * 
	 * @param keystoreFilePath
	 * @param alias
	 * @param password
	 * @return 電子署名ユーティリティオブジェクト.
	 * 
	 * @throws KeyStoreException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 */
	private DigitalSignatureUtil createSigner(File keystoreFilePath, String alias, String password)
			throws KeyStoreException, FileNotFoundException, IOException,
			NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
		DigitalSignatureUtil signer = null;
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		try (InputStream is = new FileInputStream(keystoreFilePath)) {
			keystore.load(is, password.toCharArray());
			PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());
			Certificate[] certificateChain = keystore.getCertificateChain(alias);
			signer = new DigitalSignatureUtil(privateKey, certificateChain);
		}
		return signer;
	}

	/**
	 * 電子署名ユーティリティオブジェクト作成.
	 * 
	 * 電子署名利用する公開鍵＋秘密鍵のペアと電子証明書はロジックで動的に生成しています。
	 * 
	 * @return 電子署名ユーティリティオブジェクト.
	 * 
	 * @throws KeyStoreException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException
	 * @throws SignatureException
	 */
	private DigitalSignatureUtil createSigner() throws KeyStoreException, FileNotFoundException, IOException,
			NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, InvalidKeyException,
			NoSuchProviderException, SignatureException {
		DigitalSignatureUtil signer = null;
		KeyPair keyPair = this.generateKeyPair();
		Certificate[] certificateChain = new Certificate[1];
		certificateChain[0] = this.generateCertificate(keyPair);
		signer = new DigitalSignatureUtil(keyPair.getPrivate(), certificateChain);
		return signer;
	}

	/**
	 * 公開鍵＋秘密鍵のペアを生成します.
	 * 
	 * 鍵のサイズは2048ビット。アルゴリズムはRSA。
	 * 
	 * @return 公開鍵＋秘密鍵のペア.
	 * @throws NoSuchAlgorithmException
	 */
	private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		// Bouncy Castleプロバイダーをセキュリティに追加
		Security.addProvider(new BouncyCastleProvider());

		// RSAキーペアジェネレータのインスタンスを作成
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

		// 鍵のサイズを設定（デフォルトは2048ビット）
		keyPairGenerator.initialize(2048);

		// 公開鍵と秘密鍵のペアを生成
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		return keyPair;
	}

	/**
	 * 電子証明書を生成します.
	 * 
	 * @param keyPair 公開鍵＋秘密鍵のペア.
	 * @return 電子証明書.
	 * @throws CertificateEncodingException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws SignatureException
	 */
	private X509Certificate generateCertificate(KeyPair keyPair) throws CertificateEncodingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
		try {
			// 証明書の有効期間を設定
			Date startDate = Date.from(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC));
			Date endDate = Date.from(LocalDate.now().plusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC));

			// 証明書の基本情報を設定
			X500Principal subjectDN = new X500Principal(
					"cn=madilloar, ou=mydep, o=mycorp, l=Tokyo, st=Chiyoda-ku, c=JP");
			X500Principal issuerDN = subjectDN;
			BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
			X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(issuerDN, serialNumber, startDate,
					endDate, subjectDN, keyPair.getPublic());

			// 証明書に署名するための秘密鍵を取得
			PrivateKey privateKey = keyPair.getPrivate();

			// 証明書に署名する
			ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
			X509CertificateHolder certHolder = certBuilder.build(signer);
			X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

			return cert;
		} catch (CertificateException | OperatorCreationException e) {
			throw new CertificateEncodingException("Failed to generate certificate.", e);
		}
	}

	/**
	 * PDFドキュメントに電子署名を付与します.
	 * 
	 * @param document
	 * @param output
	 * @param signer
	 * @throws IOException
	 */
	private void signDetached(PDDocument document, FileOutputStream output, DigitalSignatureUtil signer)
			throws IOException {

		PDSignature signature = new PDSignature();
		signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
		signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
		signature.setName("Your Name");
		signature.setLocation("Tokyo, Chiyoda-ku");
		signature.setReason("Testing");
		signature.setSignDate(Calendar.getInstance());

		SignatureOptions signatureOptions = new SignatureOptions();
		// signatureOptions.setVisualSignature(createVisibleSignatureProperties(document));
		document.addSignature(signature, signer, signatureOptions);

		document.saveIncremental(output);
	}

}