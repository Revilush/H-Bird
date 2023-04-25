package xbrl;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MysqlConnUtils {
	
	public static String mysqlUserName = "root";
	public static String mysqlPassword = "mysql1";
	public static String mysqlHostPort = "localhost:3306";

	
	public static Connection getConnection() {
		return getConnectionDB("contracts");
	}
	public static Connection getConnectionDB(String database) {
		Connection conn = null;
		try {
			//Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");
			// available in the mysql jar file we have added. 
			conn = DriverManager
					.getConnection(
							"jdbc:mysql://"+mysqlHostPort+"/"+database+"?allowMultiQueries=true&useSSL=false",
							mysqlUserName, mysqlPassword);
			conn.setAutoCommit(true);
			// auto commit after every sql execute
		} catch (SQLException e) {
			System.out.println("Error while getting a connection to DB:"
					+ e.getMessage());
		} catch (ClassNotFoundException e) {
			System.out.println("Error while loading DB Driver:"
					+ e.getMessage());
		}
		return conn;
	}


	public static void executeUpdateQuery(String query) throws SQLException {
		Connection conn = MysqlConnUtils.getConnection();
		executeUpdateQuery(conn, query);
		System.out.println(query);
	}

	public static void executeUpdateQuery(Connection conn, String query)
			throws SQLException {

		// System.out.println(query);
		// long startTime = System.currentTimeMillis();
		Statement stmt = conn.createStatement();
		// a new instance of Statement class conn.createStatement() only creates
		// the statement - it does not execute the statement in mySQL.
		System.out.println("/*query::: */ " + query);
		// this passes the 'query' from the caller method using the
		// 'executeUpdate' method??

		stmt.getResultSet();

		stmt.close();
		conn.close();
		// long endTime = System.currentTimeMillis();
		// System.out.println("Execution time is "
		// + ((endTime - startTime) / 1000d) + " seconds ");

	}

	// static methods are used in utilities methods b/c the utilities are
	// typically non-interdependent.
	
	public static void executeQuery(String query) throws SQLException, FileNotFoundException {
		Connection conn = MysqlConnUtils.getConnection();
		executeQuery(conn, query);
	}

	public static void executeQueryDB(String query, String database) throws SQLException, FileNotFoundException {
		Connection conn = MysqlConnUtils.getConnectionDB(database);
		
		executeQuery(conn, query);
	}

	public static void executeQuery(Connection conn, String query)
			throws SQLException, FileNotFoundException {

		// long startTime = System.currentTimeMillis();
		Statement stmt = conn.createStatement();
		// a new instance of Statement class conn.createStatement() only creates
		// the statement - it does not execute the statement in mySQL.
//		 System.out.println("/*query:: */ " + query);

		// System.out.println(query.substring(0,Math.min(300, query.length())));
		System.out.println(query);
		stmt.execute(query);

		stmt.getResultSet();
		stmt.close();
		conn.close();
		
		// long endTime = System.currentTimeMillis();
		// System.out.println("Execution time is "
		// + ((endTime - startTime) / 1000d) + " seconds ");

	}

	public static Object getScaler(String query) {
		Connection conn = null;
		Statement stmt = null;
		Object data = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			return getScaler(query, stmt);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			try {
				if (null != stmt)
					stmt.close();
				if (null != conn)
					conn.close();
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}
		System.out.println("getScaler=" + data);
		return data;
	}

	public static Object getScaler(String query, Statement stmt) {
		ResultSet rs;
		Object data = null;
		try {
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				data = rs.getObject(1); // first column..
				// System.out.println("getScaler=" + data);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public static void dropAllTmpTables() throws SQLException, FileNotFoundException{
		int cnt=0;
		Connection conn = MysqlConnUtils.getConnection();
		DatabaseMetaData metadata = conn.getMetaData();
		ResultSet rs;
		rs = metadata.getTables(null, null, "%", null);
		while (rs.next()) {
			cnt++;
			  System.out.println(rs.getString(3));
			  if(rs.getString(3).substring(0, 3).toLowerCase().equals("tmp")){
				  MysqlConnUtils.executeQuery("DROP TABLE IF EXISTS "+rs.getString(3)+";");
				  System.out.println("delete TABLE="+rs.getString(3));
			  }
			}
		System.out.println("total number of tables that remain="+cnt);
		
	}
	
	
	public static void main(String[] args) throws SQLException {

		
		
	}

}
