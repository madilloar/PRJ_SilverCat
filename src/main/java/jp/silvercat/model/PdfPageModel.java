package jp.silvercat.model;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jp.silvercat.util.IModel;
import jp.silvercat.util.IModelListener;
import jp.silvercat.util.IStatusCode;
import jp.silvercat.util.ModelEvent;
import jp.silvercat.util.PdfUtil;

public class PdfPageModel implements IModel, Icon, Cloneable, Serializable {

	private File pdfFile = null;
	private int pageNumber = 0;
	private ImageIcon pdfPageImage = null;
	private int rotation = 0;
	private final String userPassword_;

	public PdfPageModel(File pdfFile, int pageNumber, Image img, int rotation, String userPassword) {
		this.pdfFile = pdfFile;
		// 1ページ目のpageNumerは1
		this.pageNumber = pageNumber;
		this.pdfPageImage = new ImageIcon(img);
		this.rotation = rotation;
		this.userPassword_ = userPassword;
	}

	public File getPdfFile() {
		return this.pdfFile;
	}

	public ImageIcon getPdfPageImage() {
		return pdfPageImage;
	}

	public void setPdfPageImage(ImageIcon pdfPageImage) {
		this.pdfPageImage = pdfPageImage;
	}

	public void rotate() {
		this.status = STATUS_CODE.PROCESSING;

		this.rotation += 90;
		if (this.rotation > 270) {
			this.rotation = 0;
		}

		ImageIcon icon = this.getPdfPageImage();
		// 右に90度回転させるので、幅と高さを入れ替えたイメージ領域を作る
		BufferedImage bi = new BufferedImage(icon.getIconHeight(), icon.getIconWidth(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) bi.createGraphics();

		// 左上隅が原点座標。ここを軸に90度右回転すると、イメージ領域から外に
		// 描画されてしまうので、イメージの高さ分だけ右に平行移動する。
		// 90度右回転する。
		AffineTransform rotate = AffineTransform.getRotateInstance(Math.toRadians(90), 0.0, 0.0);
		// イメージの高さ分右に平衡移動する。
		AffineTransform trans = AffineTransform.getTranslateInstance(icon.getIconHeight(), 0.0);
		// 回転と移動を組み合わせる。
		trans.concatenate(rotate);
		g2d.drawImage(icon.getImage(), trans, null);
		this.setPdfPageImage(new ImageIcon(bi));

		this.status = STATUS_CODE.PROCESSEND;
		// 状態変化をリスナーに通知する。
		this.notifyListeners();
	}

	public File createTempPdfThisPage(File outputDir) throws IOException {
		this.status = STATUS_CODE.PROCESSING;
		File file = this.pdfFile;
		PdfUtil u = new PdfUtil();
		File outputPdfFile = u.createPdfThisPage(file, this.pageNumber, outputDir, this.rotation, this.userPassword_);
		this.pdfFile = outputPdfFile;
		this.pageNumber = 1;
		this.status = STATUS_CODE.PROCESSEND;
		this.notifyListeners();
		return this.pdfFile;
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
		PdfPageModel result = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			result = (PdfPageModel) ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		this.notifyListeners();
		return result;
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

	/**
	 * このオブジェクトの文字列を返します。
	 * 
	 * @return このオブジェクトの文字列を返します。
	 */
	@Override
	public String toString() {
		return this.pdfFile.toString() + "," + this.pageNumber + "," + this.rotation;
	}

	/**
	 * ハッシュコードを返します。 このオブジェクトを文字列化して、その文字列のハッシュコードを返します。
	 * 
	 * @return ハッシュコードを返します。
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	//
	// Icon インターフェースの実装。
	// JListにPDFページのサムネイル画像を表示するためのインターフェース。
	//
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		this.pdfPageImage.paintIcon(c, g, x, y);
	}

	@Override
	public int getIconWidth() {
		return this.pdfPageImage.getIconWidth();
	}

	@Override
	public int getIconHeight() {
		return this.pdfPageImage.getIconHeight();
	}

	//
	// IModelインターフェースの実装
	//
	private List<IModelListener> listeners = new CopyOnWriteArrayList<IModelListener>();
	private ModelEvent event = null;
	private IStatusCode status = null;

	public enum STATUS_CODE implements IStatusCode {
		PROCESSING, PROCESSEND;
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
		this.status = STATUS_CODE.PROCESSEND;
		this.notifyListeners();
	}

}
