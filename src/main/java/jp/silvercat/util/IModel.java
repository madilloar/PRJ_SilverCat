package jp.silvercat.util;

public interface IModel {
  public void addModelListener(IModelListener l);

  public void notifyListeners();

  public IStatusCode getStatus();

  public void removeModelListner(IModelListener l);

  public void close();
}
