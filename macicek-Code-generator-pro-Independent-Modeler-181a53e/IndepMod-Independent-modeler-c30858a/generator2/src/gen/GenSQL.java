package gen;

import cz.cvut.indepmod.classmodel.api.model.IAttribute;
import cz.cvut.indepmod.classmodel.api.model.ICardinality;
import cz.cvut.indepmod.classmodel.api.model.IElement;
import cz.cvut.indepmod.classmodel.api.model.IClassModelModel;
import cz.cvut.indepmod.classmodel.api.model.IRelation;
import cz.cvut.indepmod.classmodel.api.model.RelationType;
import integration.OutputJavaClass;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * @Author Pavel Macenauer
 */
public class GenSQL implements IGen{

    private IClassModelModel myModel;
    private OutputJavaClass out = null;
    private String suffix = ".java";
    
    // Struktury
    private Set<String> Tables = new HashSet<String>();
    private HashMap<String, Set<String>> Attributes = new HashMap<String, Set<String>>();
    private HashMap<String, Set<String>> PrimaryKeys = new HashMap<String, Set<String>>();       
    private HashMap<String, Set<FK>> ForeignKeys = new HashMap<String, Set<FK>>();
    private HashMap<String, Set<String>> Unique = new HashMap<String, Set<String>>();
    
    // ForeignKey structure
    /**
     * table
     *      reference_table
     *              attribute REFERENCE TO reference_table(attribute)
     */
    public class FK
    {    
        public String ref_table;
        public HashMap<String, String> attr;        
    }         
    
    public GenSQL(IClassModelModel model) {
        System.out.println("Predan model.");
        myModel = model;
    }

    @Override
    public void generateModel(String save_path) throws IOException {
        System.out.println("Zacatek generovani SQL.");        

        genTables();
        writeSqlTables(true);
        
        System.out.println("Generovani SQL dokonceno.");        
    }          
    
// ---------------------- METODY PRO GENEROVANI STRUKTURY ----------------------
    
    /**
     * Vybere jmena vsech tabulek a vlozi je do seznamu.
     * Soucasne i rodicovska funkce dalsich veci jako atributy, zavislosti, atd.
     */
    public void genTables()
    {
        for (IElement element : myModel.getClasses())
        {
            Tables.add(element.toString());
            genAttributes(element);        
            genRelations(element);         
        }
    }
    
    /**
     * Vybere jmena vsech atributu dane tridy a prida je do seznamu vazaneho na danou tridu     
     */
    public void genAttributes(IElement element)
    {
        Set<String> attrSet = new HashSet<String>();   // seznam atributu
        Set<String> pkSet = new HashSet<String>();     // seznam primarnich klicu
        for (IAttribute iAttribute : element.getAttributeModels())   // projde vsechny atributy kazde tridy
        {            
            attrSet.add(iAttribute.toString());            
            if (isPrimaryKey(iAttribute.toString()))
            {
                pkSet.add(iAttribute.toString());
            }
        }
        // vytvori seznamy atributu a primarni klicu vazane na jmena dane tabulky
        Attributes.put(element.toString(), attrSet);
        PrimaryKeys.put(element.toString(), pkSet);
    }
    
    /**
     * Vygeneruje constrainty, tabulky, atributy podle typu jednotlivych relaci     
     */
    public void genRelations(IElement element)
    {
        for (IRelation relation : element.getRelatedClass()) 
        {
            IElement start = relation.getStartingClass();
            IElement end = relation.getEndingClass();
            RelationType type = relation.getRelationType();
            Cardinality startCardinality = filterCardinality(relation.getStartCardinality());
            Cardinality endCardinality = filterCardinality(relation.getEndCardinality());
            
            switch (type) 
            {
                /**
                 * RELACE
                 */
                case RELATION:
                    // N-N relace
                    // 1-1 relace
                    if ((startCardinality == Cardinality.ZERO_N && endCardinality == Cardinality.ZERO_N)
                     || (startCardinality == Cardinality.ONE_ONE && endCardinality == Cardinality.ONE_ONE)) 
                    {
                        String tableName = start.toString() + "_" + end.toString(); // jmeno relacni tabulky
                        
                        Tables.add(tableName); // prida mezi tabulky
                                                
                        Set<String> relAttr = new HashSet<String>();
                        Set<String> relUnique = new HashSet<String>();
                        Set<FK> relForeignKeys = new HashSet<FK>();
                        FK foreignKey = new FK();                        
                        foreignKey.ref_table = start.toString();
                        // vytvori atributy a unique constraints pro relacni tabulku z primarnich klicu startovni relace                        
                        for (String pk : PrimaryKeys.get(start.toString()))   
                        {
                            String attrName = start.toString() + "_" + pk;  // jmeno atributu
                            relAttr.add(attrName);                          // atributy
                            relUnique.add(attrName);                        // unique constraints                                                       
                            foreignKey.attr.put(attrName, pk);              // cizi klic
                        }
                        relForeignKeys.add(foreignKey);
                        
                        // vytvori atributy a unique constraints pro relacni tabulku z primarnich klicu koncove relace                        
                        foreignKey = new FK();
                        foreignKey.ref_table = end.toString();
                        for (String pk: PrimaryKeys.get(end.toString()))
                        {
                            String attrName = end.toString() + "_" + pk;    // jmeno atributu
                            relAttr.add(attrName);                          // atributy
                            relUnique.add(attrName);                        // unique constraints
                            foreignKey.attr.put(attrName, pk);              // cizi klic                                                        
                        }
                        relForeignKeys.add(foreignKey);
                        
                        // prida jednotlive struktury do globalnich map
                        Attributes.put(tableName, relAttr);
                        Unique.put(tableName, relUnique);
                        ForeignKeys.put(tableName, relForeignKeys);
                        break;
                    }
                    // 1-N relace
                    if (startCardinality == Cardinality.ONE_ONE && endCardinality == Cardinality.ZERO_N) 
                    {                        
                        // prida mezi atributy N entity primarni klice 1 entity a udela zaznam o cizim klici                        
                        Set<String> endAttr = Attributes.get(end.toString());
                        Set<FK> relForeignKeys = new HashSet<FK>();
                        FK foreignKey = new FK();
                        foreignKey.ref_table = start.toString();
                        for (String pk: PrimaryKeys.get(start.toString()))
                        {
                            String attrName = start.toString() + "_" + pk;
                            endAttr.add(attrName);                            
                            foreignKey.attr.put(attrName, pk);    
                        }
                        relForeignKeys.add(foreignKey);
                        
                        // udela zaznam o cizim klici                        
                        ForeignKeys.put(end.toString(), relForeignKeys);
                        break;
                    }
                    break;
                    
                /**
                 * Kompozice
                 */
                case COMPOSITION:                        
                    break;
            }
        }
    }
    
// --------------------------- METODY PRO VYPIS SQL ----------------------------
    
    public void writeSqlTables(boolean drop) throws IOException
    {
        String filename = "generated_db_schema.sql"; // jmeno souboru do ktereho budem zapisovat
        FileWriter fstream = new FileWriter(filename);
        BufferedWriter output = new BufferedWriter(fstream);
        
        // drop
        if (drop)
        {
            for (String table : Tables)        
                output.write("DROP TABLE " + table.toString() + ";" + Globals.nl);
        }
        output.write(Globals.nl);
        
        // create
        for (String table : Tables)
        {
            output.write("CREATE TABLE " + table.toString() + " (" + Globals.nl);
            writeSQLAttributes(output, table);
            writeSQLConstraints(output, table);
            output.write(");" + Globals.nl);
        }        
        
        output.close();
    }
    
    public void writeSQLAttributes(BufferedWriter output, String table) throws IOException
    {
        for (String attr : Attributes.get(table))        
            output.write(attr + "," + Globals.nl);        
    }
    
    public void writeSQLConstraints(BufferedWriter output, String table) throws IOException
    {   
        // primarni klice
        output.write("CONSTRAINT PK_" + table + " "); // jmeno constraintu
        output.write("PRIMARY KEY (");
        for (String pk : PrimaryKeys.get(table))
        {
            output.write(pk + ", ");
        }
        output.write(")");
                
        output.write(Globals.nl);
        
        // unikatni atributy
        output.write("CONSTRAINT UNQ_" + table + " "); // jmeno uniqu
        output.write("UNIQUE (");
        for (String unq : Unique.get(table))
        {
            output.write(unq + ", ");
        }
        output.write(")");
        
        output.write(Globals.nl);
        
        // cizi klice
        int count = 1;
        for (FK fk : ForeignKeys.get(table))
        {            
            output.write("CONSTRAINT FK_" + table + "_" + count); // jmeno constraintu                
            output.write(" FOREIGN KEY ("); // atributy
            for (String foreignKey : fk.attr.keySet())
                output.write(foreignKey + ", ");
                        
            output.write("REFERENCES " + fk.ref_table + "("); // reference
            for (String foreignKey : fk.attr.keySet())
                output.write(fk.attr.get(foreignKey));
            
            count++;
        }        
    }
    
    
// ----------------------------- POMOCNE METODY --------------------------------
    
    public boolean isPrimaryKey(String input)
    {                
        if (input.startsWith("pk_"))
            return true;
        return false;           
    }
    
    private Cardinality filterCardinality(ICardinality cardin) {
        Cardinality result = Cardinality.ERROR;

        if (cardin.getFrom() == 1 && cardin.getTo() == -1) {
            result = Cardinality.ONE_N;
        } else if (cardin.getFrom() == 1 && cardin.getTo() == 1) {
            result = Cardinality.ONE_ONE;
        } else if (cardin.getFrom() == 0 && cardin.getTo() == -1) {
            result = Cardinality.ZERO_N;
        }

        return result;
    }
    
/******************************************************************************/
    
   /*
    private void generateSql(BufferedWriter output, IElement c) throws IOException {
        try {        
            writeSqlTable(output, c);
            writeSqlRelations(output, c);
        } catch (IOException e) {
            throw e; // pripadnou psani preda dal
        }
    }
    
 
    
  
    private void writeSqlTable(BufferedWriter output, IElement c) throws IOException {
        output.write(Globals.nl);
        // ---

        output.write("DROP TABLE " + c.toString() + ";");
        output.write(Globals.nl);
        output.write("CREATE TABLE " + c.toString() + " (");
        output.write(Globals.nl);
        writeSqlAttributes(output, c);
        output.write(Globals.nl);
        output.write(");");

        // ---
        output.write(Globals.nl);
    }


    private void writeSqlAttributes(BufferedWriter output, IElement c) throws IOException {
        for (IAttribute attribute : c.getAttributeModels()) {
            output.write(attribute.getName() + " " + attribute.getType().toString() + ",");
            output.write(Globals.nl);
        }
        output.write("PRIMARY KEY (");
            this.writePrimaryKeys(output, c);
        output.write(")");
    }




    private void writeSqlRelations(BufferedWriter output, IElement c) throws IOException {
        for (IRelation rel : c.getRelatedClass()) {
            // zkraceni jednotlivych nazvu pro prehlednost
            IElement start = rel.getStartingClass();
            IElement end = rel.getEndingClass();
            RelationType type = rel.getRelationType();
            Cardinality start_rel = filterCardinality(rel.getStartCardinality());
            Cardinality end_rel = filterCardinality(rel.getEndCardinality());            

   
            // IAttribute pk_1;
            // IAttribute pk_2;
            output.write(Globals.nl);
            // ---
            switch (type) {
                case RELATION:
                    // N-N relace
                    if ((start_rel == Cardinality.ZERO_N && end_rel == Cardinality.ZERO_N)
                            || (start_rel == Cardinality.ONE_ONE && end_rel == Cardinality.ONE_ONE)) {
                        output.write("DROP TABLE " + start.toString() + "_" + end.toString() + ";");
                        output.write(Globals.nl);
                        output.write("CREATE TABLE " + start.toString() + "_" + end.toString() + " (");
                        output.write(Globals.nl);
                        output.write(start.toString());
                        output.write("_jmeno_pk_1" + " " + "typ_pk1");
                        output.write(Globals.nl);
                        output.write(end.toString());
                        output.write("_jmeno_pk_2" + " " + "typ_pk2");
                        output.write(Globals.nl);
                        output.write(");");
                        break;
                    }
                    // 1-N relace
                    if (start_rel == Cardinality.ONE_ONE && end_rel == Cardinality.ZERO_N) {
                        output.write("ALTER TABLE " + end.toString() + " ADD (");
                        output.write(start.toString());
                        output.write("_jmeno_pk_1" + " " + "typ_pk1");
                        output.write(Globals.nl);
                        output.write(");");
                    }
                    break;
                case COMPOSITION:
                    output.write("ALTER TABLE " + end.toString() + " ADD (");
                    output.write(Globals.nl);
                    // output.write(pk.getName()+" "+pk.getType().toString()+",");
                    output.write(Globals.nl);
                    output.write("PRIMARY KEY (");
                        this.writePrimaryKeys(output, c);
                    output.write(")");
                    output.write(Globals.nl);
                    output.write(");");
                    break;
            }
            // ---
            output.write(Globals.nl);

        }
    }        */
            
}