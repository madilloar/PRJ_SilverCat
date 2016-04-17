package jp.silvercat;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import jp.silvercat.model.PdfFileModel;
import jp.silvercat.model.PdfPageModel;
import jp.silvercat.util.IModelListener;
import jp.silvercat.util.ModelEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//public class PdfViewModel implements IModel {
public class PdfViewModel {
  private static final Logger LOG = LoggerFactory.getLogger(PdfViewModel.class);

  private PdfViewModel self_ = null;

  private final ExecutorService ex = Executors.newSingleThreadExecutor();

  // ViewModel
  // 編集元PDFファイルリスト
  protected final DefaultTableModel originalPdfFileTableModel = new DefaultTableModel();
  protected ListSelectionModel originalPdfFileListSelectionModel = null;

  // 編集元となるPDFファイルのページイメージのリスト
  protected final DefaultListModel<PdfPageModel> originalPdfPageImageListModel = new DefaultListModel<PdfPageModel>();
  protected ListSelectionModel originalPdfPageImageListSelectionModel = null;

  // 編集先のPDFファイルのページイメージリスト
  protected final DefaultListModel<PdfPageModel> editPdfPageImageListModel = new DefaultListModel<PdfPageModel>();
  protected ListSelectionModel editPdfPageImageSelectionModel = null;

  // ProgressBar用
  protected DefaultBoundedRangeModel progressBarModel = null;

  // Viewに公開する処理群
  protected final MenuOpenHandler menuOpenHandler = new MenuOpenHandler();
  protected final FileOpenHandler fileOpenHandler = new FileOpenHandler();
  protected final ExitHandler exitHandler = new ExitHandler();
  protected final OriginalPdfFileListSelectionHandler originalPdfFileListSelectionHandler = new OriginalPdfFileListSelectionHandler();
  protected final SelectAllOriginalPdfPageListHandler selectAllOriginalPdfPageListHandler = new SelectAllOriginalPdfPageListHandler();
  protected final UnSelectAllOriginalPdfPageListHandler unSelectAllOriginalPdfPageListHandler = new UnSelectAllOriginalPdfPageListHandler();
  protected final AddEditPdfPageListHandler addEditPdfPageListHandler = new AddEditPdfPageListHandler();
  protected final DeleteEditPdfPageListHandler deleteEditPdfPageListHandler = new DeleteEditPdfPageListHandler();
  protected final RotateEditPdfPageListHandler rotateEditPdfPageListHandler = new RotateEditPdfPageListHandler();
  protected final CreatePdfHandler createPdfHandler = new CreatePdfHandler();

  /**
   * コンストラクタ
   */
  public PdfViewModel() {
    this.self_ = this;
    //
    Object[] columnNames = new String[] { "ファイル名", "ページ数", "ファイルパス" };
    this.originalPdfFileTableModel.setColumnIdentifiers(columnNames);

  }

  //
  //
  // Modelを操作するメソッド群
  //
  //
  private synchronized void addPdfFile(final File file) {
    // Modelの更新には時間がかかるので、別スレッドを使う。
    ex.execute(new Runnable() {
      public void run() {
        // callbackの定義
        IModelListener ml = new IModelListener() {
          public void modelChanged(ModelEvent event) {
            PdfFileModel model = (PdfFileModel) event.getSource();

            // PDFファイルのページ画像作成中はLOADINGステータス。
            // その間は、プログレスバーの状態を更新する。
            if (model.getStatus().equals(PdfFileModel.STATUS_CODE.LOADING)) {
              self_.progressBarModel.setMaximum(model.getTotalPageNumber());
              self_.progressBarModel.setValue(model.getProcessingPageNumber());
              return;
            }

            // プログレスバーを100%にする。
            self_.progressBarModel.setMaximum(model.getTotalPageNumber());
            self_.progressBarModel.setValue(model.getTotalPageNumber());

            // PDFファイルリストに行追加
            Object[] rowData = model.getRow();
            self_.originalPdfFileTableModel.addRow(rowData);

            // PDFファイルのページサムネイルをリフレッシュ。
            self_.redrawOriginalPdfPage(model);

            // もう監視が不要なので登録解除。
            model.removeModelListner(this);
          }
        };
        // Modelの新規作成
        final PdfFileModel model = new PdfFileModel();
        // Modelに監視役のリスナー（callback)を登録
        model.addModelListener(ml);
        // 時間のかかる処理。サムネイル画像を作るので。
        model.load(file);
      }
    });

  }

  /**
   * オリジナルPDFファイルページサムネイル画像を再描画します。
   * 
   * @param model
   */
  private void redrawOriginalPdfPage(PdfFileModel model) {
    // ListModel#clear()をすると、at
    // javax.swing.plaf.basic.BasicListUI.updateLayoutState(BasicListUI.java:1351)
    // でNullPointerExceptionが発生する。
    // JListが参照しているデータがなくなるので、セルの高さの計算ができなくてNullPoとなっている模様。
    // JList#setFixedCellHeight()でセルの高さに固定値をセットしたら解消した。
    self_.originalPdfPageImageListModel.clear();

    List<PdfPageModel> pages = model.getPages();
    for (PdfPageModel pdfPageModel : pages) {
      self_.originalPdfPageImageListModel.addElement(pdfPageModel);
    }
  }

  //
  //
  // Viewに公開する処理のクラス群
  //
  //
  protected class MenuOpenHandler implements MenuListener {
    public final String KEY = "ファイル(F)";

    @Override
    public void menuSelected(MenuEvent event) {
      LOG.debug(KEY + ":selected" + event);
    }

    @Override
    public void menuDeselected(MenuEvent e) {
      LOG.debug(KEY + ":deselected" + e);
    }

    @Override
    public void menuCanceled(MenuEvent e) {
      LOG.debug(KEY + ":cancleled" + e);
    }
  }

  /**
   * PDFファイルを開く。 ファイルダイアログが表示されるので、Viewに持ち込むべきロジックかもしれない。
   */
  @SuppressWarnings("serial")
  protected class FileOpenHandler extends AbstractAction {
    public final String KEY = "PDFファイルを開く(O)...";

    public FileOpenHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
    }

    public synchronized void actionPerformed(ActionEvent event) {
      LOG.debug(KEY + ":" + event);
      JFileChooser filechooser = new JFileChooser(new File(".").getPath());
      filechooser.setMultiSelectionEnabled(true);
      filechooser.setDialogTitle("PDFファイル開くダイアログ");

      FileFilter filter = new FileNameExtensionFilter("PDFファイル", "PDF");
      filechooser.setFileFilter(filter);

      int selected = filechooser.showOpenDialog(null);
      if (selected == JFileChooser.APPROVE_OPTION) {
        File[] files = filechooser.getSelectedFiles();
        // Modelに対する操作
        for (File file : files) {
          int beginIndex = file.getName().lastIndexOf('.');
          if (beginIndex < 0) {
            return;
          }
          String ext = file.getName().substring(beginIndex, file.getName().length()).toUpperCase();
          if (ext.equals(".PDF")) {
            self_.addPdfFile(file);
          }
        }
      }
    }
  }

  /**
   * PDFファイルを開く。 ファイルダイアログが表示されるので、Viewに持ち込むべきロジックかもしれない。
   */
  @SuppressWarnings("serial")
  protected class ExitHandler extends AbstractAction {
    public final String KEY = "終了(X)";

    public ExitHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
    }

    public synchronized void actionPerformed(ActionEvent event) {
      LOG.debug(KEY + ":" + event);
    }
  }

  /**
   * オリジナルPDFのページを全て選択します。
   */
  @SuppressWarnings("serial")
  protected class SelectAllOriginalPdfPageListHandler extends AbstractAction {
    public final String KEY = "↑全選択(A)↑";

    public SelectAllOriginalPdfPageListHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
    }

    public void actionPerformed(ActionEvent event) {
      if (!KEY.equals(event.getActionCommand())) {
        return;
      }
      if (self_.originalPdfPageImageListSelectionModel == null) {
        return;
      }

      ListSelectionModel lsm = self_.originalPdfPageImageListSelectionModel;
      lsm.clearSelection();
      int maxIndex = self_.originalPdfPageImageListModel.size() - 1;
      lsm.setSelectionInterval(0, maxIndex);
    }
  }

  /**
   * オリジナルPDFのページの選択を全て解除します。
   */
  @SuppressWarnings("serial")
  protected class UnSelectAllOriginalPdfPageListHandler extends AbstractAction {
    public final String KEY = "↑全選択解除↑(U)";

    public UnSelectAllOriginalPdfPageListHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
    }

    public void actionPerformed(ActionEvent event) {
      if (!KEY.equals(event.getActionCommand())) {
        return;
      }
      if (self_.originalPdfPageImageListSelectionModel == null) {
        return;
      }
      ListSelectionModel lsm = self_.originalPdfPageImageListSelectionModel;
      lsm.clearSelection();
    }
  }

  /**
   * オリジナルPDFのページを編集エリアに追加します。
   */
  @SuppressWarnings("serial")
  protected class AddEditPdfPageListHandler extends AbstractAction {
    public final String KEY = "↓追加(P)";

    public AddEditPdfPageListHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
    }

    public void actionPerformed(ActionEvent event) {
      if (!KEY.equals(event.getActionCommand())) {
        return;
      }
      if (self_.originalPdfPageImageListSelectionModel == null) {
        return;
      }

      if (self_.originalPdfPageImageListSelectionModel.isSelectionEmpty()) {
        ;// NOP
      } else {
        // 重くなりそうな処理なので別スレッドで動かす。
        ex.execute(new Runnable() {
          public void run() {
            // progress bar を使うほど遅くなかったのでリスナーは使わない。

            int minIndex = self_.originalPdfPageImageListSelectionModel.getMinSelectionIndex();
            int maxIndex = self_.originalPdfPageImageListSelectionModel.getMaxSelectionIndex();
            for (int i = minIndex; i <= maxIndex; i++) {
              if (self_.originalPdfPageImageListSelectionModel.isSelectedIndex(i)) {
                PdfPageModel selectedItem = (PdfPageModel) self_.originalPdfPageImageListModel.get(i);
                PdfPageModel copy = selectedItem.deepClone();
                self_.editPdfPageImageListModel.addElement(copy);
              }
            }
          }
        });
      }
    }
  }

  /**
   * PDFファイル編集エリアから選択されたページを削除する。
   */
  @SuppressWarnings("serial")
  protected class DeleteEditPdfPageListHandler extends AbstractAction {
    public final String KEY = "↑削除(D)";

    public DeleteEditPdfPageListHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);

    }

    public void actionPerformed(ActionEvent event) {
      if (!KEY.equals(event.getActionCommand())) {
        return;
      }
      if (self_.editPdfPageImageSelectionModel == null) {
        return;
      }

      if (self_.editPdfPageImageSelectionModel.isSelectionEmpty()) {
        ;// NOP
      } else {
        int minIndex = self_.editPdfPageImageSelectionModel.getMinSelectionIndex();
        int maxIndex = self_.editPdfPageImageSelectionModel.getMaxSelectionIndex();
        for (int i = minIndex; i <= maxIndex; i++) {
          if (self_.editPdfPageImageSelectionModel.isSelectedIndex(i)) {
            self_.editPdfPageImageListModel.remove(i);
          }
        }
      }
    }
  }

  /**
   * PDFファイル編集エリアの選択されたページを右に90度回転する。
   */
  @SuppressWarnings("serial")
  protected class RotateEditPdfPageListHandler extends AbstractAction {
    public final String KEY = "→90度回転(R)";

    public RotateEditPdfPageListHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
    }

    public void actionPerformed(ActionEvent event) {
      if (!KEY.equals(event.getActionCommand())) {
        return;
      }
      if (self_.editPdfPageImageSelectionModel == null) {
        return;
      }

      if (self_.editPdfPageImageSelectionModel.isSelectionEmpty()) {
        ;// NOP
      } else {
        int minIndex = self_.editPdfPageImageSelectionModel.getMinSelectionIndex();
        int maxIndex = self_.editPdfPageImageSelectionModel.getMaxSelectionIndex();
        for (int i = minIndex; i <= maxIndex; i++) {
          if (self_.editPdfPageImageSelectionModel.isSelectedIndex(i)) {
            final int index = i;
            // 選択されたページを取り出す。
            final PdfPageModel ppm = (PdfPageModel) self_.editPdfPageImageListModel.get(index);
            // ページ回転処理結果を受けるcallbackを定義。
            IModelListener iml = new IModelListener() {
              public void modelChanged(ModelEvent event) {
                PdfPageModel mod = (PdfPageModel) event.getSource();
                // 返ってきたcallbackでPDFファイル編集エリアのページを再度描画しなおす。
                // 実際にはセットしなおすことで、再描画を促している。
                // TODO:若干メモリリーク臭あり。再度setしなおしたときに、
                // ダングリングポインタになりそう
                self_.editPdfPageImageListModel.set(index, mod);
                // 処理が終わったので監視解除。
                mod.removeModelListner(this);
              }
            };
            // ページ回転を監視。
            ppm.addModelListener(iml);

            // Modelの状態を変化させる(ページ回転)。
            // Modelの状態変化に時間がかかるかもなので、別スレッドで動かす。
            ex.execute(new Runnable() {
              public void run() {
                ppm.rotate();
              }
            });
          }
        }
      }
    }
  }

  /**
   * 編集した結果をPDFファイルとして保存します。
   */
  @SuppressWarnings("serial")
  protected class CreatePdfHandler extends AbstractAction {
    public final String KEY = "◎PDFファイル作成(C)";

    public CreatePdfHandler() {
      putValue(Action.NAME, KEY);
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
    }

    public void actionPerformed(ActionEvent event) {
      if (!KEY.equals(event.getActionCommand())) {
        return;
      }

      ex.execute(new Runnable() {
        public void run() {
          if (self_.editPdfPageImageListModel == null) {
            return;
          }
          if (self_.editPdfPageImageListModel.getSize() < 1) {
            return;
          }
          JFileChooser filechooser = new JFileChooser(new File(".").getPath());
          filechooser.setMultiSelectionEnabled(false);
          filechooser.setDialogTitle("PDFファイル保存ダイアログ");
          filechooser.setApproveButtonText("保存");
          filechooser.setApproveButtonToolTipText("ファイル名テキストボックスで指定されたファイル名でPDFファイルを保存します。");

          FileFilter ff = new FileNameExtensionFilter("PDFファイル", "PDF");
          filechooser.setFileFilter(ff);

          File newFile = null;
          int selected = filechooser.showOpenDialog(null);
          if (selected == JFileChooser.APPROVE_OPTION) {
            newFile = filechooser.getSelectedFile();
            int beginIndex = newFile.getName().lastIndexOf('.');
            if (beginIndex < 0) {
              return;
            }
            String ext = newFile.getName().substring(beginIndex, newFile.getName().length()).toUpperCase();
            if (!ext.equals(".PDF")) {
              return;
            }
          }

          // STEP1:PdfFileModelを作ります。まだ各ページはPDF化されていません
          final PdfFileModel newPdfFile = new PdfFileModel(newFile);
          final int pagesSize = self_.editPdfPageImageListModel.getSize();
          Object[] o = self_.editPdfPageImageListModel.toArray();

          List<?> listPpm = Arrays.asList(o);
          newPdfFile.addPage(listPpm);

          // STEP2:各ページのPDFファイルを作り、それらを1つのPDFファイルにします。
          try {
            self_.progressBarModel.setMaximum(pagesSize);
            self_.progressBarModel.setValue(0);
            newPdfFile.addModelListener(new IModelListener() {
              public void modelChanged(ModelEvent event) {
                PdfFileModel model = (PdfFileModel) event.getSource();
                int processPage = self_.progressBarModel.getValue();
                processPage++;
                self_.progressBarModel.setValue(processPage);
                if (model.getStatus().equals(PdfFileModel.STATUS_CODE.TEMP_PDF_PAGE_PROCESSING)) {
                  return;
                }
                if (model.getStatus().equals(PdfFileModel.STATUS_CODE.TEMP_PDF_PAGE_PROCESSEND)) {
                  self_.progressBarModel.setValue(self_.progressBarModel.getMaximum() / 2);
                  return;
                }
                if (processPage >= pagesSize || model.getStatus().equals(PdfFileModel.STATUS_CODE.CREATE_PDF_FILE_PROCESSEND)) {
                  self_.progressBarModel.setValue(self_.progressBarModel.getMaximum());
                  newPdfFile.removeModelListner(this);
                }
              }
            });
            // 時間のかかる処理
            newPdfFile.createPdfFile();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    }
  }

  /**
   * オリジナルPDFファイルリストで選択された行のページを再描画する。
   */
  protected class OriginalPdfFileListSelectionHandler implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent event) {
      boolean isAdjusting = event.getValueIsAdjusting();
      // リストをクリックして離したときは直ぐにreturn
      if (!isAdjusting) {
        return;
      }
      if (self_.originalPdfFileListSelectionModel.isSelectionEmpty()) {
        ; // NOP
      } else {
        int minIndex = self_.originalPdfFileListSelectionModel.getMinSelectionIndex();
        int maxIndex = self_.originalPdfFileListSelectionModel.getMaxSelectionIndex();
        for (int i = minIndex; i <= maxIndex; i++) {
          if (self_.originalPdfFileListSelectionModel.isSelectedIndex(i)) {
            @SuppressWarnings("rawtypes")
            Vector row = (Vector) self_.originalPdfFileTableModel.getDataVector().elementAt(i);
            for (Object col : row) {
              if (col instanceof PdfFileModel) {
                PdfFileModel model = (PdfFileModel) col;
                self_.redrawOriginalPdfPage(model);
              }
            }
          }
        }
      }
    }
  }

  // IModelインターフェースの実装
  // 以下、コメントアウトにした。
  // 理由は、PdfViewModelオブジェクトを監視するオブジェクトがいないから。
  // Viewが通知を受け取って状態を変更することを考えたが、
  // PdfViewModelオブジェクトが保持しているSwingコンポーネントをViewが監視しているので、
  // あえて何かする必要がないため。
  //
  // private EventListenerList listeners = new EventListenerList();
  // private ModelEvent event = null;
  // private STATUS_CODE status = null;
  //
  // public enum STATUS_CODE {
  // START, END
  // }
  //
  // public STATUS_CODE getStatus() {
  // return status;
  // }
  //
  // public void addModelListener(IModelListener l) {
  // this.listeners.add(IModelListener.class, l);
  // }
  //
  // public void removeModelListner(IModelListener l) {
  // this.listeners.remove(IModelListener.class, l);
  // }
  //
  // protected void modelChanged() {
  // // Guaranteed to return a non-null array
  // Object[] listeners = this.listeners.getListenerList();
  // // Process the listeners last to first, notifying
  // // those that are interested in this event
  // for (int i = listeners.length - 2; i >= 0; i -= 2) {
  // if (listeners[i] == IModelListener.class) {
  // // Lazily create the event:
  // if (event == null)
  // event = new ModelEvent(this);
  // ((IModelListener) listeners[i + 1]).modelChanged(event);
  // }
  // }
  // }
}
