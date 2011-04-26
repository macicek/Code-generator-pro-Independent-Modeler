package cz.cvut.indepmod.classmodel.persistence.xml.delegate;

import cz.cvut.indepmod.classmodel.diagramdata.DiagramDataModel;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;

/**
 * Date: 19.2.2011
 * Time: 12:43:47
 * @author Lucky
 */
public class ClassModelDiagramModelPersistenceDelegate extends DefaultPersistenceDelegate {

    @Override
    protected Expression instantiate(Object oldInstance, Encoder out) {
        DiagramDataModel c = (DiagramDataModel)oldInstance;
        return new Expression(
                oldInstance,
                oldInstance.getClass(),
                "new",
                new Object[]{c.getLayoutCache(), c.getDiagramType()});
    }
}
