/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package enoloxianeodatismongooracle;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.Where;

/**
 *
 * @author oracle
 */
public class Enoloxianeodatismongooracle {

    /**
     * @param args the command line arguments
     */
    static Connection conn;
    static ResultSet result;
    public static final String ODB_NAME = "vinho";
    public static ODB odb = null;
    public static MongoClient client;
    public static MongoDatabase database;
    public static MongoCollection<Document> coleccion;
    public static String nomeuva, tipouva, codigo, trataAcidez, dni;
    public static int acidez, total;

    public static void main(String[] args) throws SQLException {
        // TODO code application logic here

        odb = ODBFactory.open(ODB_NAME);
        conexion();

        lectura_analisis_vinho(odb);
        amosar_minmax_uva();
        //actualizar_analisis_clientes(odb);
        lectura_actualizacion_cliente(odb);
        añadir_datos_xerado_mongo(odb);

        odb.close();
        conn.close();
    }

    //conexion a la base oracle
    public static void conexion() {
        try {
            String driver = "jdbc:oracle:thin:";
            String host = "localhost.localdomain";
            String porto = "1521";
            String sid = "orcl";
            String usuario = "hr";
            String password = "hr";
            String url = driver + usuario
                    + "/" + password + "@" + host
                    + ":" + porto + ":" + sid;
            conn = DriverManager.getConnection(url);
            System.out.println("Base de datos operativa. Conectado");
        } catch (SQLException ex) {
            Logger.getLogger(Enoloxianeodatismongooracle.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //mostrar analisis da base de datos vinho
    public static void lectura_analisis_vinho(ODB odb) {

        Objects<Analisis> analise = odb.getObjects(Analisis.class);

        Analisis analisis = null;

        while (analise.hasNext()) {
            analisis = analise.next();
            System.out.println("Codigo:" + analisis.getCodigoa()
                    + " Acidez:" + analisis.getAcidez()
                    + " Tipo:" + analisis.getTipouva()
                    + " Cantidade:" + analisis.getCantidade()
                    + " DNI:" + analisis.getDni());
        }
    }

    //amosar acidez min/max a cada uva analizada
    public static void amosar_minmax_uva() {
        try {
            PreparedStatement ver = conn.prepareStatement("Select * from uva");
            result = ver.executeQuery();
            while (result.next()) {
                System.out.println("TIPO  " + ":" + result.getString("tipouva"));
                System.out.println("NOME  " + ":" + result.getString("nomeu"));
                System.out.println("ACMIN  " + ":" + result.getInt("acidezmin"));
                System.out.println("ACMAX  " + ":" + result.getInt("acidezmax"));
            }
        } catch (SQLException ex) {
            System.out.println("Error, verificar que la base este conectada " + ex.getMessage());
        }
    }

    //actualizar analisis dos clientes
    public static void actualizar_analisis_clientes(ODB odb) {

        Objects<Analisis> analisis = odb.getObjects(Analisis.class);
        IQuery query;
        Analisis analise = null;
        Cliente clientes = null;

        while (analisis.hasNext()) {
            analise = analisis.next();
            query = odb.criteriaQuery(Cliente.class, Where.equal("dni", analise.getDni()));
            clientes = (Cliente) odb.getObjects(query).getFirst();
            clientes.setNumerodeanalisis(clientes.getNumerodeanalisis() + 1);
            odb.store(clientes);
        }
    }

    //mostrar la actualizacion de los analisis
    public static void lectura_actualizacion_cliente(ODB odb) {

        Objects<Cliente> clientes = odb.getObjects(Cliente.class);

        Cliente clientete = null;

        while (clientes.hasNext()) {
            clientete = clientes.next();
            System.out.println("DNI:" + clientete.getDni()
                    + " Nome:" + clientete.getNome()
                    + " Telf:" + clientete.getTelf()
                    + " Analisis:" + clientete.getNumerodeanalisis());
        }
    }

    //añadir los datos a mongo
    public static void añadir_datos_xerado_mongo(ODB odb) throws SQLException {

        String consulta = "select nomeu,acidezmin,acidezmax from uva where tipouva=?";
        PreparedStatement ps;
        ps = conn.prepareStatement(consulta);

        Objects<Analisis> analisis = odb.getObjects(Analisis.class);
        Analisis analise = null;
        while (analisis.hasNext()) {

            analise = analisis.next();
            ps.setString(1, analise.getTipouva());
            result = ps.executeQuery();
            result.next();

            acidez = analise.getAcidez();
            dni = analise.getDni();
            total = analise.getCantidade() * 15;
            codigo = analise.getCodigoa();

            int min = Integer.parseInt(result.getNString("acidezmin"));
            int max = Integer.parseInt(result.getNString("acidezmax"));
            nomeuva = result.getNString("nomeu");

            if (acidez < min) {
                trataAcidez = "subir acidez";
            } else if (acidez > max) {
                trataAcidez = "bajar acidez";
            } else {
                trataAcidez = "correcta";
            }

            client = new MongoClient("localhost", 27017);
            database = client.getDatabase("resultado2");
            coleccion = database.getCollection("xerado2");

            Document docu = new Document("_id", codigo)
                    .append("uva", nomeuva)
                    .append("tratacidez", trataAcidez)
                    .append("total", total);
            coleccion.insertOne(docu);

        }
    }

}
