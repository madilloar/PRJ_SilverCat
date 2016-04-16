package jp.silvercat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;

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

import jp.silvercat.model.PdfPageModel;

public class PdfView extends JFrame {

  private PdfViewModel viewModel_;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          PdfViewModel viewModel = new PdfViewModel();
          PdfView view = new PdfView(viewModel);
          view.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the application.
   */
  public PdfView(PdfViewModel viewModel) {

    this.viewModel_ = viewModel;
    initialize();
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    setBounds(100, 100, 800, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout(0, 0));

    JMenuBar menuBar = new JMenuBar();
    this.setJMenuBar(menuBar);

    JMenu mnFile = new JMenu("File");
    menuBar.add(mnFile);

    JMenuItem mntmFileOpen = new JMenuItem();
    mntmFileOpen.setAction(this.viewModel_.fileOpenHandler);
    mnFile.add(mntmFileOpen);

    JPanel pnlOriginal = new JPanel();
    pnlOriginal.setLayout(new BorderLayout(0, 0));

    JTable jtblPdfFiles = new JTable(this.viewModel_.originalPdfFileTableModel);
    jtblPdfFiles.setDefaultEditor(Object.class, null);
    this.viewModel_.originalPdfFileListSelectionModel = jtblPdfFiles.getSelectionModel();
    jtblPdfFiles.getSelectionModel().addListSelectionListener(this.viewModel_.originalPdfFileListSelectionHandler);
    JScrollPane scrlpnOriginalPdfFiles = new JScrollPane(jtblPdfFiles);
    scrlpnOriginalPdfFiles.setPreferredSize(new Dimension(640, 100));
    scrlpnOriginalPdfFiles.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrlpnOriginalPdfFiles.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    pnlOriginal.add(scrlpnOriginalPdfFiles, BorderLayout.NORTH);

    final JList<PdfPageModel> jlstOriginal = new JList<PdfPageModel>();
    jlstOriginal.setBackground(Color.LIGHT_GRAY);
    jlstOriginal.setFixedCellHeight(150);
    jlstOriginal.setVisibleRowCount(1);
    jlstOriginal.setLayoutOrientation(JList.VERTICAL_WRAP);
    jlstOriginal.setModel(this.viewModel_.originalPdfPageImageListModel);
    this.viewModel_.originalPdfPageImageListSelectionModel = jlstOriginal.getSelectionModel();

    JScrollPane scrlpnOriginalPdfPageImages = new JScrollPane(jlstOriginal);
    scrlpnOriginalPdfPageImages.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrlpnOriginalPdfPageImages.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pnlOriginal.add(scrlpnOriginalPdfPageImages, BorderLayout.CENTER);

    JPanel pnlOriginalButtonArea = new JPanel();
    pnlOriginal.add(pnlOriginalButtonArea, BorderLayout.SOUTH);
    JButton btnOriginalAllSelect = new JButton();
    btnOriginalAllSelect.setAction(this.viewModel_.selectAllOriginalPdfPageListHandler);
    pnlOriginalButtonArea.add(btnOriginalAllSelect);

    JButton btnAllUnSelect = new JButton();
    btnAllUnSelect.setAction(this.viewModel_.unSelectAllOriginalPdfPageListHandler);
    pnlOriginalButtonArea.add(btnAllUnSelect);

    JButton btnAdd = new JButton();
    btnAdd.setAction(this.viewModel_.addEditPdfPageListHandler);
    pnlOriginalButtonArea.add(btnAdd);

    JPanel pnlEdit = new JPanel();
    pnlEdit.setLayout(new BorderLayout(0, 0));
    JList<PdfPageModel> jlstEdit = new JList<PdfPageModel>();
    jlstEdit.setBackground(Color.LIGHT_GRAY);
    jlstEdit.setFixedCellHeight(150);
    jlstEdit.setVisibleRowCount(1);
    jlstEdit.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    jlstEdit.setModel(this.viewModel_.editPdfPageImageListModel);
    this.viewModel_.editPdfPageImageSelectionModel = jlstEdit.getSelectionModel();
    JScrollPane scrlpnEdit = new JScrollPane(jlstEdit);
    scrlpnEdit.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrlpnEdit.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pnlEdit.add(scrlpnEdit);

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlOriginal, pnlEdit);
    splitPane.setDividerLocation(300);

    JPanel pnlEditButtonArea = new JPanel();
    pnlEdit.add(pnlEditButtonArea, BorderLayout.SOUTH);

    JButton btnRemove = new JButton();
    btnRemove.setAction(this.viewModel_.removeEditPdfPageListHandler);
    pnlEditButtonArea.add(btnRemove);

    JButton btnRotate = new JButton();
    btnRotate.setAction(this.viewModel_.rotateEditPdfPageListHandler);
    pnlEditButtonArea.add(btnRotate);

    JButton btnCreate = new JButton();
    btnCreate.setAction(this.viewModel_.createPdfHandler);
    pnlEditButtonArea.add(btnCreate);

    getContentPane().add(splitPane, BorderLayout.CENTER);

    JProgressBar progressBar = new JProgressBar();
    // TODO:progress bar の setModel(model)やコンストラクタにmodelを渡すやり方だと
    // ViewのDesign画面でNullpointerExceptionが発生するので、Viewから取得したモデルを与えている。
    this.viewModel_.progressBarModel = (DefaultBoundedRangeModel) progressBar.getModel();
    getContentPane().add(progressBar, BorderLayout.SOUTH);
    progressBar.setStringPainted(true);

  }
}
