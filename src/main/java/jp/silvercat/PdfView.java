package jp.silvercat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import jp.silvercat.util.IModelListener;
import jp.silvercat.util.ModelEvent;

public class PdfView extends JFrame implements IModelListener {

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          PdfViewModel viewModel = new PdfViewModel();
          PdfView view = new PdfView(viewModel);
          viewModel.addModelListener(view);
          view.setVisible(true);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the application.
   */
  public PdfView(PdfViewModel viewModel) {

    initialize(viewModel);
  }

  /**
   * Initialize the contents of the frame.
   */
  @SuppressWarnings("unchecked")
  private void initialize(PdfViewModel viewModel) {
    setBounds(100, 100, 800, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout(0, 0));

    JMenuBar menuBar = new JMenuBar();
    this.setJMenuBar(menuBar);

    JMenu mnFile = new JMenu();
    mnFile.addMenuListener(viewModel.menuOpenHandler);
    mnFile.setMnemonic(KeyEvent.VK_1);
    mnFile.setText("ファイル(1)"); // ALT+Fが反応しないので、ALT+1にした。
    menuBar.add(mnFile);

    // ファイルを開く(O)
    JMenuItem mntmFileOpen = new JMenuItem();
    mntmFileOpen.setAction(viewModel.fileOpenHandler);
    mnFile.add(mntmFileOpen);

    // 終了(X)
    JMenuItem mntmExit = new JMenuItem();
    mntmExit.setAction(viewModel.exitHandler);
    mnFile.add(mntmExit);

    JPanel pnlOriginal = new JPanel();
    pnlOriginal.setLayout(new BorderLayout(0, 0));

    // PDF ファイルのテーブル
    JTable jtblPdfFiles = new JTable(viewModel.originalPdfFileTableModel);
    jtblPdfFiles.setDefaultEditor(Object.class, null);
    viewModel.originalPdfFileListSelectionModel = jtblPdfFiles.getSelectionModel();
    jtblPdfFiles.getSelectionModel().addListSelectionListener(viewModel.originalPdfFileListSelectionHandler);
    JScrollPane scrlpnOriginalPdfFiles = new JScrollPane(jtblPdfFiles);
    scrlpnOriginalPdfFiles.setPreferredSize(new Dimension(640, 100));
    scrlpnOriginalPdfFiles.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrlpnOriginalPdfFiles.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    pnlOriginal.add(scrlpnOriginalPdfFiles, BorderLayout.NORTH);

    // 入力PDFファイルのサムネイル画像のリスト
    @SuppressWarnings("rawtypes")
    final JList jlstOriginal = new JList();
    jlstOriginal.setBackground(Color.LIGHT_GRAY);
    // ListModel#clear()をすると、at
    // javax.swing.plaf.basic.BasicListUI.updateLayoutState(BasicListUI.java:1351)
    // でNullPointerExceptionが発生する。
    // JListが参照しているデータがなくなるので、セルの高さの計算ができなくてNullPoとなっている模様。
    // JList#setFixedCellHeight()でセルの高さに固定値をセットしたら解消した。
    jlstOriginal.setFixedCellHeight(150);
    jlstOriginal.setVisibleRowCount(1);
    jlstOriginal.setLayoutOrientation(JList.VERTICAL_WRAP);
    jlstOriginal.setModel(viewModel.originalPdfPageImageListModel);
    viewModel.originalPdfPageImageListSelectionModel = jlstOriginal.getSelectionModel();

    JScrollPane scrlpnOriginalPdfPageImages = new JScrollPane(jlstOriginal);
    scrlpnOriginalPdfPageImages.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrlpnOriginalPdfPageImages.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pnlOriginal.add(scrlpnOriginalPdfPageImages, BorderLayout.CENTER);

    // 全選択ボタン
    JPanel pnlOriginalButtonArea = new JPanel();
    pnlOriginal.add(pnlOriginalButtonArea, BorderLayout.SOUTH);
    JButton btnOriginalAllSelect = new JButton();
    btnOriginalAllSelect.setAction(viewModel.selectAllOriginalPdfPageListHandler);
    pnlOriginalButtonArea.add(btnOriginalAllSelect);

    // 全選択解除ボタン
    JButton btnAllUnSelect = new JButton();
    btnAllUnSelect.setAction(viewModel.unSelectAllOriginalPdfPageListHandler);
    pnlOriginalButtonArea.add(btnAllUnSelect);

    // 編集エリアへの追加ボタン
    JButton btnAdd = new JButton();
    btnAdd.setAction(viewModel.addEditPdfPageListHandler);
    pnlOriginalButtonArea.add(btnAdd);

    // 編集エリアのサムネイル画像のリスト
    JPanel pnlEdit = new JPanel();
    pnlEdit.setLayout(new BorderLayout(0, 0));
    @SuppressWarnings("rawtypes")
    JList jlstEdit = new JList();
    jlstEdit.setBackground(Color.LIGHT_GRAY);
    // ListModel#clear()をすると、at
    // javax.swing.plaf.basic.BasicListUI.updateLayoutState(BasicListUI.java:1351)
    // でNullPointerExceptionが発生する。
    // JListが参照しているデータがなくなるので、セルの高さの計算ができなくてNullPoとなっている模様。
    // JList#setFixedCellHeight()でセルの高さに固定値をセットしたら解消した。
    jlstEdit.setFixedCellHeight(150);
    jlstEdit.setVisibleRowCount(1);
    jlstEdit.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    jlstEdit.setModel(viewModel.editPdfPageImageListModel);
    viewModel.editPdfPageImageSelectionModel = jlstEdit.getSelectionModel();
    JScrollPane scrlpnEdit = new JScrollPane(jlstEdit);
    scrlpnEdit.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrlpnEdit.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pnlEdit.add(scrlpnEdit);

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlOriginal, pnlEdit);
    splitPane.setDividerLocation(300);

    JPanel pnlEditButtonArea = new JPanel();
    pnlEdit.add(pnlEditButtonArea, BorderLayout.SOUTH);

    // 削除ボタン
    JButton btnDelete = new JButton();
    btnDelete.setAction(viewModel.deleteEditPdfPageListHandler);
    pnlEditButtonArea.add(btnDelete);

    // 回転ボタン
    JButton btnRotate = new JButton();
    btnRotate.setAction(viewModel.rotateEditPdfPageListHandler);
    pnlEditButtonArea.add(btnRotate);

    // PDFファイル作成ボタン
    JButton btnCreate = new JButton();
    btnCreate.setAction(viewModel.createPdfHandler);
    pnlEditButtonArea.add(btnCreate);

    getContentPane().add(splitPane, BorderLayout.CENTER);

    JProgressBar progressBar = new JProgressBar();
    // TODO:progress bar の setModel(model)やコンストラクタにmodelを渡すやり方だと
    // ViewのDesign画面でNullpointerExceptionが発生するので、Viewから取得したモデルを与えている。
    viewModel.progressBarModel = (DefaultBoundedRangeModel) progressBar.getModel();
    getContentPane().add(progressBar, BorderLayout.SOUTH);
    progressBar.setStringPainted(true);

  }

  //
  // IModelListener
  //
  @Override
  public void modelChanged(ModelEvent event) {
    PdfViewModel model = (PdfViewModel) event.getSource();
    // ViewModelからの通知がENDならば、アプリケーションを終了する。
    if (model.getStatus().equals(PdfViewModel.STATUS_CODE.END)) {
      System.exit(0);
    }
  }

}
