package cz.cvut.indepmod.classmodel.persistence.xml.delegate;

import cz.cvut.indepmod.classmodel.diagramdata.DiagramDataModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.AnotationAttributeModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.Cardinality;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.EnumerationModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.HierarchyRelationModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.InterfaceModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.RelationModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.ClassModel;
import cz.cvut.indepmod.classmodel.Common;
import cz.cvut.indepmod.classmodel.api.model.DiagramType;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.AnotationModel;
import java.util.Set;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.AttributeModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.AbstractElementModel;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.MethodModel;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import cz.cvut.indepmod.classmodel.workspace.cell.model.classModel.TypeModel;
import java.beans.ExceptionListener;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lucky
 */
public class PersistenceDelegatesTest {

    private File f;
    private XMLEncoder encoder;

    @Before
    public void setUp() {
        this.f = new File(Common.FILE_NAME);

        try {
            this.encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(f)));
            this.encoder.setPersistenceDelegate(DiagramDataModel.class, new ClassModelDiagramModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(AbstractElementModel.class, new AbstractElementlPersistenceDelegate());
            this.encoder.setPersistenceDelegate(AttributeModel.class, new AttributeModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(MethodModel.class, new MethodModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(TypeModel.class, new TypeModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(RelationModel.class, new RelationModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(HierarchyRelationModel.class, new HierarchyRelationModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(AnotationModel.class, new AnotationModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(AnotationAttributeModel.class, new AnotationAtributeModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(Cardinality.class, new CardinalityPersistenceDelegate());
            this.encoder.setPersistenceDelegate(ClassModel.class, new ClassModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(InterfaceModel.class, new InterfaceModelPersistenceDelegate());
            this.encoder.setPersistenceDelegate(EnumerationModel.class, new EnumerationModelPersistenceDelegate());
            this.encoder.setExceptionListener(new ExceptionListener() {

                @Override
                public void exceptionThrown(Exception e) {
                    fail("Error during encoding, probably there is missing some of Persistence Delegates: " + e.getMessage());
                }
            });
        } catch (FileNotFoundException ex) {
            fail();
        }
    }

    @After
    public void tearDown() {
        f.delete();
    }

    /**
     * This test tries to encode, decode and verify TypeModel class
     */
    @Test
    public void testTypeModelEncodeDecode() {
        TypeModel tm = new TypeModel(Common.TYPE_NAME);
        TypeModel dtm = null;

        this.encoder.writeObject(tm);
        this.encoder.close();

        dtm = (TypeModel) this.decode(this.f);

        assertEquals(Common.TYPE_NAME, dtm.getTypeName());
    }

    /**
     * This test tries to encode, decode and verify AbstractElementModel class
     */
    @Test
    public void testClassModelEncodeDecode() {
        AbstractElementModel cm = new ClassModel(Common.CLASS_NAME, Common.getMethods(), Common.getAttributes(), Common.getAnotations());
        AbstractElementModel dcm = null;

        this.encoder.writeObject(cm);
        this.encoder.close();

        dcm = (AbstractElementModel) this.decode(this.f);

        assertEquals(Common.CLASS_NAME, dcm.getTypeName());
        assertEquals(3, dcm.getAttributeModels().size());
        assertEquals(2, dcm.getMethodModels().size());
        assertEquals(3, dcm.getAnotations().size());
    }

    @Test
    /**
     * This test tries to encode, decode and verify AttributeModel class
     */
    public void testAttributeModelEncodeDecode() {
        AttributeModel am = new AttributeModel(new TypeModel(Common.TYPE_NAME), Common.ATTRIBUTE_NAME);
        AttributeModel dam = null;

        this.encoder.writeObject(am);
        this.encoder.close();

        dam = (AttributeModel) this.decode(this.f);
        assertEquals(Common.TYPE_NAME, dam.getType().getTypeName());
        assertEquals(Common.ATTRIBUTE_NAME, dam.getName());
    }

    @Test
    /**
     * This test tries to encode, decode and verify MethodModel class
     */
    public void testMethodModelEncodeDecode() {
        Set<AttributeModel> attributes = new HashSet<AttributeModel>();

        MethodModel mm = new MethodModel(new TypeModel(Common.TYPE_NAME), Common.METHOD_NAME, Common.getAttributes());
        MethodModel dmm = null;

        this.encoder.writeObject(mm);
        this.encoder.close();

        dmm = (MethodModel) this.decode(this.f);

        assertEquals(Common.TYPE_NAME, mm.getType().getTypeName());
        assertEquals(Common.METHOD_NAME, dmm.getName());
        assertEquals(3, dmm.getAttributeModels().size());
    }
    
    @Test
    public void testDiagramDataEncodeDecode() {
        int classCounter = 10;
        int interfaceCounter = 42;
        int enumCounter = 21;
        DiagramType diagType;
        
        DiagramDataModel model = new DiagramDataModel();
        model.setClassCounter(classCounter);
        model.setInterfaceCounter(interfaceCounter);
        model.setEnumerationCounter(enumCounter);
        diagType = model.getDiagramType();
        
        this.encoder.writeObject(model);
        this.encoder.close();
        
        
        model = (DiagramDataModel) this.decode(this.f);
        assertEquals(classCounter, model.getClassCounter());
        assertEquals(interfaceCounter, model.getInterfaceCounter());
        assertEquals(enumCounter, model.getEnumerationCounter());
        assertEquals(diagType, model.getDiagramType());
        assertNotNull(diagType);
    }

    //========================== PRIVATE METHODS ===============================

    private Object decode(File f) {
        try {
            XMLDecoder dec = new XMLDecoder(new BufferedInputStream(new FileInputStream(this.f)));
            if (dec != null) {
                Object obj = dec.readObject();
                dec.close();
                return obj;
            }
        } catch (FileNotFoundException ex) {
            fail(ex.getMessage());
        }
        return null;
    }
}
