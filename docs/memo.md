# 2023/05/27(土):itextからの脱却
文書を開くパスワード付きPDFファイルを読めるように対応した。
ただし、いまいちいけてないインスタンス変数に持ってしまっている。どうするのがよいだろうか？

# 2023/05/26(金):keytoolを使って電子証明書の元ネタを作る
https://svn.apache.org/viewvc/pdfbox/branches/2.0/examples/src/main/java/org/apache/pdfbox/examples/signature/

C:\pleiades-2022-12\java\11\bin\keytool -genkeypair -alias mycert -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -dname "cn=madilloar, ou=mydep, o=mycorp, l=Tokyo, st=Chiyoda-ku, c=JP" -validity 3560 -storetype PKCS12 -keystore C:\Users\lemac\keystore.p12 -storepass password!

# 2023/05/16 ChatGPTを使ってPDFファイルに電子署名するプロンプト
公開鍵暗号にはRSAアルゴリズムを使用してX509電子証明書をPEM形式で出力し、公開鍵をPEM形式で出力し、秘密鍵をパスワードで暗号化してPEM形式で出力するJavaコードを生成してください。
Javaコード生成時には、必要なメソッドをstep by stepで生成してください。

PEM形式のX509電子証明書を利用してPDFファイルに電子署名するJavaコードを生成してください。
PDFBoxライブラリBuncyCastleライブラリを利用してください。


# 2014/01/01 12:50
## 事象:
オリジナルPDFファイルのページのサムネイルイメージを表示しているエリアを全選択した後に、
新たにPDFファイルを読むと下記のようなNullPointerExceptionが発生する。
PdfViewModel#redrawOriginalPdfPage()で、サムネイルイメージを表示しているJListが参照しているListModelをclear()すると、この例外が発生する。

## 解決策:
スタックトレースの1351行を見ると、fixedCellHeighが-1の時、JListのセルの高さを設定するようである。

## 問題のコード:
<pre><code class="java">
if (fixedCellHeight == -1) {
  cellHeights[index] = cellSize.height;
}
</code>

JListのsetPreferSize()で指定された値でセルの高さを自動設定する場合、fixedCellHeighが"-1"となるようである(JListのAPIドキュメントより)。
試しに、setPreferSizeを指定してみたが、状況は変わらなかった。セルの高さを自動設定しているのがダメ見たい。
なので、JList#setFixedCellHeight()でセルの高さに固定値をセットしたところ例外は発生しなくなった。

<pre><code class="java">
    Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException
        at javax.swing.plaf.basic.BasicListUI.updateLayoutState(BasicListUI.java:1351)
        at javax.swing.plaf.basic.BasicListUI.maybeUpdateLayoutState(BasicListUI.java:1294)
        at javax.swing.plaf.basic.BasicListUI.getCellBounds(BasicListUI.java:935)
        at javax.swing.plaf.basic.BasicListUI.paintImpl(BasicListUI.java:286)
        at javax.swing.plaf.basic.BasicListUI.paint(BasicListUI.java:222)
        at javax.swing.plaf.ComponentUI.update(ComponentUI.java:143)
        at javax.swing.JComponent.paintComponent(JComponent.java:760)
        at javax.swing.JComponent.paint(JComponent.java:1037)
        at javax.swing.JComponent.paintToOffscreen(JComponent.java:5132)
        at javax.swing.RepaintManager$PaintManager.paintDoubleBuffered(RepaintManager.java:1523)
        at javax.swing.RepaintManager$PaintManager.paint(RepaintManager.java:1454)
        at javax.swing.RepaintManager.paint(RepaintManager.java:1257)
        at javax.swing.JComponent._paintImmediately(JComponent.java:5080)
        at javax.swing.JComponent.paintImmediately(JComponent.java:4890)
        at javax.swing.RepaintManager$3.run(RepaintManager.java:814)
        at javax.swing.RepaintManager$3.run(RepaintManager.java:802)
        at java.security.AccessController.doPrivileged(Native Method)
        at java.security.AccessControlContext$1.doIntersectionPrivilege(AccessControlContext.java:87)
        at javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:802)
        at javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:745)
        at javax.swing.RepaintManager.prePaintDirtyRegions(RepaintManager.java:725)
        at javax.swing.RepaintManager.access$1000(RepaintManager.java:46)
        at javax.swing.RepaintManager$ProcessingRunnable.run(RepaintManager.java:1668)
        at java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:209)
        at java.awt.EventQueue.dispatchEventImpl(EventQueue.java:672)
        at java.awt.EventQueue.access$400(EventQueue.java:81)
        at java.awt.EventQueue$2.run(EventQueue.java:633)
        at java.awt.EventQueue$2.run(EventQueue.java:631)
        at java.security.AccessController.doPrivileged(Native Method)
        at java.security.AccessControlContext$1.doIntersectionPrivilege(AccessControlContext.java:87)
        at java.awt.EventQueue.dispatchEvent(EventQueue.java:642)
        at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:269)
        at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:184)
        at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:174)
        at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:169)
        at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:161)
        at java.awt.EventDispatchThread.run(EventDispatchThread.java:122)
    java.lang.InterruptedException
        at java.lang.Object.wait(Native Method)
        at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:118)
        at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:134)
        at sun.java2d.Disposer.run(Disposer.java:127)
        at java.lang.Thread.run(Thread.java:662)
    Exception while removing reference: java.lang.InterruptedException
</code>

#2014/01/01 17:01
JList#setPreferSize()を指定すると、横スクロールバーが出ない。
