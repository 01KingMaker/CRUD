/*
    author: Fabien@KM -
*/

package DAO;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

public class Crud {

    String prefixe = "";
    int longPK = 7;
    String nomFonction = "getSeqPRS";
    Boolean primaryKey = false;

    String bUser = "postgres";
    String bPass = "postgres";
    String bUrl = "jdbc:postgresql://localhost:5432/";
    String base = "ticketing";

    public Vector getObject(ResultSet resultSet) throws Exception {
        Field[] fields = this.getClass().getDeclaredFields();
        Vector<Object> vObjects = new Vector<>();
        while (resultSet.next()) {
            Constructor<?> constructor = this.getClass().getDeclaredConstructor();
            Object ob = constructor.newInstance();
            try {
                int i = 1;
                for (Field field : fields) {
                    String typeF = field.getType() + "";
                    String name = field.getName();
                    String method = getSetter(name);
                    Method m = this.getClass().getDeclaredMethod(method, Object.class);
                    m.invoke(ob, resultSet.getObject(i));
                    i += 1;
                }
            } catch (Exception e) {
                
            }
            vObjects.add(ob);
        }
        return vObjects;
    }

    public String getCondition() {
        String condition = " WHERE ";
        Field[] g = this.getClass().getDeclaredFields();
        try {
            for (int i = 0; i < g.length; i++) {
                Method m = this.getClass().getMethod(getGetter(g[i].getName()));
                Object temp = m.invoke(this);
                if (temp != null) {
                    g[i].setAccessible(true);
                    condition = condition + g[i].getName() + "='" + g[i].get(this) + "'";
                    try {
                        Object o = (this.getClass().getMethod(getGetter(g[i + 1].getName()))).invoke(this);
                        if (o != null) {
                            condition += " and ";
                        }
                    } catch (Exception exc) {
                    }
                }
            }
        } catch (Exception ee) {
        }
        return condition;
    }

    public static String getSetter(String str) {
        str = firstLetterToUpper(str);
        return "set" + str;
    }

    public static String getGetter(String str) {
        str = firstLetterToUpper(str);
        return "get" + str;
    }

    public static String firstLetterToUpper(String str) {
        String retour = str.charAt(0) + "";
        retour = retour.toUpperCase();
        for (int i = 1; i < str.length(); i++) {
            retour += str.charAt(i) + "";
        }
        return retour;
    }

    public Connection enterToBdd() {
        try {
            Connection c = DriverManager.getConnection(bUrl+base, bUser, bPass);
            c.setAutoCommit(false);
            return c;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String stringValues() {
        Field[] g = this.getClass().getDeclaredFields();
        String values = "";
        try {
            for (Field field : g) {
                if (field.getType() == java.sql.Date.class) {
                    values = values + "to_Date('" + field.get(this) + "','yyyy-mm-dd'),";
                } else if (field.getType() == ArrayList.class) {
                    //Don't insert it
                } else if (field.getType() == Vector.class) {
                    //Don't insert it
                } else {
                    values = values + "'" + field.get(this) + "',";
                }
            }
        } catch (Exception ee) {
        }
        values = values.substring(0, values.length() - 1);
        return values;
    }

    
    public String construirePK(Connection c) throws SQLException {
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
        }
        int sequence = this.getSequence(c);
        String pk = this.getPrefixe();
        int nb = this.getPrefixe().length();
        int reste = this.getLongPK() - nb;
        String num = this.mameno(sequence, reste);
        return this.getPrefixe() + num;
    }

    public String mameno(int numero, int reste) {
        String num = numero + "";
        String zero = "";
        reste = reste - num.length();
        for (int i = 0; i < reste; i++) {
            zero = zero + "0";
        }
        String retour = zero + num;
        return retour;
    }

    public int getSequence(Connection c) throws SQLException {
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
        }
        String sql = "Select " + this.getNomFonction() + " FROM DUAL";
        Statement statement = c.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        while (resultSet.next()) {
            return resultSet.getInt(1);
        }
        return 0;
    }

    public String getSetter(Object o) {
        String condition = " set ";
        Field[] g = o.getClass().getDeclaredFields();
        try {
            for (int i = 0; i < g.length; i++) {
                Method m = o.getClass().getMethod(getGetter(g[i].getName()));
                Object temp = m.invoke(o);
                if (temp != null) {
                    g[i].setAccessible(true);
                    condition = condition + g[i].getName() + "='" + g[i].get(o) + "'";
                    try {
                        Object o1 = (o.getClass().getMethod(getGetter(g[i + 1].getName()))).invoke(o);
                        if (o1 != null) {
                            condition += " , ";
                        }
                    } catch (Exception exc) {
                    }
                }
            }
        } catch (Exception ee) {
        }
        return condition;
    }

    public void insert(Connection c) throws Exception {
        boolean mine = true;
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
            mine = false;
        }
        String values = this.stringValues();
        String query = "INSERT INTO " + this.getClass().getSimpleName() + " VALUES(" + values + ")";
        Statement statement = c.createStatement();
        int x = statement.executeUpdate(query);
        try {
            c.commit();
        } catch (Exception ee) {
            c.rollback();
        }
        if (!mine) {
            c.close();
        }
    }

    public void update(Connection c, Object o) throws Exception {
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
        }
        Statement statement = c.createStatement();
        String setter = this.getSetter(o);
        String condition = this.getCondition();
        String query = "UPDATE " + this.getClass().getSimpleName() + setter + condition;
        System.out.println(query);
        int x = statement.executeUpdate(query);
        try {
            c.commit();
            System.out.println(x+" rows modified");
        } catch (Exception ee) {
            c.rollback();
        }
        c.close();
    }

    public void delete(Connection c) throws Exception {
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
        }
        Statement statement = c.createStatement();
        String condition = this.getCondition();
        String query = "DELETE FROM " + this.getClass().getSimpleName() + condition;
        System.out.println(query);
        int x = statement.executeUpdate(query);
        try {
            c.commit();
            System.out.println(x+" rows modified");
        } catch (Exception ee) {
            c.rollback();
        }
        c.close();
    }

    public Vector select(Connection c) throws Exception {
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
        }
        String query = "Select * from " + this.getClass().getSimpleName();
        String condition = this.getCondition();
        Statement statement = c.createStatement();
        query = query + " " + condition;
        ResultSet resultSet = statement.executeQuery(query);
        Vector vObjects = this.getObject(resultSet);
        c.close();
        return vObjects;
    }

    public Vector selectAll(Connection c) throws Exception {
        if (c == null || c.isClosed()) {
            c = this.enterToBdd();
        }
        String query = "Select * from " + this.getClass().getSimpleName();
        String condition = this.getCondition();
        Statement statement = c.createStatement();
        query = query;
        ResultSet resultSet = statement.executeQuery(query);
        Vector vObjects = this.getObject(resultSet);
        c.close();
        return vObjects;
    }

    public void setPrefixe(String prefixe) {
        this.prefixe = prefixe;
    }

    public String getPrefixe() {
        return prefixe;
    }

    public void setLongPK(int longPK) {
        this.longPK = longPK;
    }

    public int getLongPK() {
        return longPK;
    }

    public void setNomFonction(String nomFonction) {
        this.nomFonction = nomFonction;
    }

    public String getNomFonction() {
        return nomFonction;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public String getbUser() {
        return bUser;
    }

    public void setbUser(String bUser) {
        this.bUser = bUser;
    }

    public String getbPass() {
        return bPass;
    }

    public void setbPass(String bPass) {
        this.bPass = bPass;
    }

    public String getbUrl() {
        return bUrl;
    }

    public void setbUrl(String bUrl) {
        this.bUrl = bUrl;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getBase() {
        return base;
    }
    
    

}
