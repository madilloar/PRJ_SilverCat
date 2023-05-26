package jp.silvercat.signature;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class RSAKeyPairGenerator {
	public static void main(String[] args) {
		try {
			 // Bouncy Castleプロバイダーをセキュリティに追加
	        Security.addProvider(new BouncyCastleProvider());

			// RSAキーペアジェネレータのインスタンスを作成
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

			// 鍵のサイズを設定（デフォルトは2048ビット）
			keyPairGenerator.initialize(2048);

			// 公開鍵と秘密鍵のペアを生成
			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			// X509電子証明書の生成
			X509Certificate cert = generateCertificate(keyPair);

			// PEM形式でX509電子証明書を出力
			saveCertificateAsPEM(cert, "certificate.pem");

			// PEM形式で公開鍵を出力
			savePublicKeyAsPEM(keyPair.getPublic(), "public_key.pem");

			// パスワードで秘密鍵を暗号化し、PEM形式で出力
			saveEncryptedPrivateKeyAsPEM(keyPair.getPrivate(), "password", "private_key.pem");
			System.out.println("証明書、公開鍵、秘密鍵がファイルに保存されました。");
		} catch (NoSuchAlgorithmException | IOException | CertificateException | InvalidKeyException
				| SignatureException | NoSuchProviderException e) {
			e.printStackTrace();
		}
	}

	private static X509Certificate generateCertificate(KeyPair keyPair) throws CertificateEncodingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
		try {
			// 証明書の有効期間を設定
			Date startDate = Date.from(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC));
			Date endDate = Date.from(LocalDate.now().plusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC));

			// 証明書の基本情報を設定
			X500Principal subjectDN = new X500Principal("cn=madilloar, ou=mydep, o=mycorp, l=Tokyo, st=Chiyoda-ku, c=JP");
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

	private static void saveCertificateAsPEM(X509Certificate certificate, String filePath) throws IOException, CertificateEncodingException {
		// PEM形式の証明書を作成
		StringWriter writer = new StringWriter();
		PemWriter pemWriter = new PemWriter(writer);
		PemObject pemObject = new PemObject("CERTIFICATE", certificate.getEncoded());
		pemWriter.writeObject(pemObject);
		pemWriter.close();

		// PEM形式の証明書をファイルに保存
		FileOutputStream fos = new FileOutputStream(filePath);
		fos.write(writer.toString().getBytes());
		fos.close();
	}

	private static void savePublicKeyAsPEM(PublicKey publicKey, String filePath) throws IOException {
		try (StringWriter stringWriter = new StringWriter();
				JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {

			pemWriter.writeObject(publicKey);
			pemWriter.flush();

			try (FileOutputStream fos = new FileOutputStream(filePath);
					OutputStreamWriter osw = new OutputStreamWriter(fos)) {
				osw.write(stringWriter.toString());
			}
		}
	}

	private static void saveEncryptedPrivateKeyAsPEM(PrivateKey privateKey, String password, String filePath)
			throws IOException {
		try (StringWriter stringWriter = new StringWriter();
				JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {

			// 秘密鍵をPKCS#8形式のバイト配列に変換
			PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());

			// PKCS#8形式のバイト配列から秘密鍵オブジェクトを復元
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			Key privateKeyObj = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

			// PEM形式の暗号化された秘密鍵を作成
			JcePEMEncryptorBuilder encryptorBuilder = new JcePEMEncryptorBuilder("DES-EDE3-CBC");
			encryptorBuilder.setProvider("BC");
			PEMEncryptor pemEncryptor = encryptorBuilder.build(password.toCharArray());

			// PEM形式で暗号化された秘密鍵を出力
			pemWriter.writeObject(privateKeyObj, pemEncryptor);
			pemWriter.flush();

			// PEM形式の秘密鍵をファイルに保存
			try (FileOutputStream fos = new FileOutputStream(filePath);
					OutputStreamWriter osw = new OutputStreamWriter(fos)) {
				osw.write(stringWriter.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
