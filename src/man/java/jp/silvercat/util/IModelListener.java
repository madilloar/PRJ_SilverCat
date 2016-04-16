package jp.silvercat.util;

import java.util.EventListener;

public interface IModelListener extends EventListener{
    public void modelChanged(ModelEvent event);
}
