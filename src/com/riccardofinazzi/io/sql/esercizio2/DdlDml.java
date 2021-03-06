package com.riccardofinazzi.io.sql.esercizio2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;

import utils.SqlConnection;

/**
 * standard(ish) UN*X preferable sysexits codes have been choosen due to missing
 * Windows's equivalent (https://stackoverflow.com/a/31521351).
 */
public class DdlDml extends SqlConnection {

	private static final String		$POPULATE_FILE_PATH	= "src/utils/viaggi.sql";

	public DdlDml( String url, String user, String password) {
		super(url,user,password);
	}

	public static void main( String[] args) {

		DdlDml o = new DdlDml("jdbc:mysql://localhost:3306?useSSL=false", "root", "root");

		o.connect();
		if( o.isConnected()) {
			try {
				o.createDatabase();
			} catch( SQLException e) {
				e.printStackTrace();
			}
			o.disconnect();
		}

		o = new DdlDml("jdbc:mysql://localhost:3306/viaggi?useSSL=false", "root", "root");

		o.connect();
		if( o.isConnected()) {
			try {
				o.ddl();
				o.populate();
				o.dml();
			} catch( SQLException e) {
				e.printStackTrace();
			}
			o.disconnect();
		}

		System.out.println("end of program.");
	}

	public void createDatabase() throws SQLException {

		try( Statement s = conn.createStatement()) {
			s.executeUpdate("CREATE DATABASE IF NOT EXISTS viaggi CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;");
		}
	}

	public void ddl() throws SQLException {

		try( Statement s = conn.createStatement()) {
			s.addBatch("CREATE TABLE IF NOT EXISTS vacanza (codice INT, descrizione VARCHAR(32));");
			s.addBatch("CREATE TABLE IF NOT EXISTS cliente (codice INT, nome VARCHAR(32), cognome VARCHAR(32), codicefiscale VARCHAR(32), datadinascita DATE, datadiregistrazione TIMESTAMP, recapitotel VARCHAR(32), email VARCHAR(32), PRIMARY KEY(codice));");
			s.addBatch("CREATE TABLE IF NOT EXISTS villeggiante (codicefiscale VARCHAR(32), nome VARCHAR(32), vacanza INT);");
			s.executeBatch();
		}
		/* utilizzando il comando ALTER TABLE, imponete sullo schema definito al
		 * punto precedente i seguenti vincoli */
		try( Statement s = conn.createStatement()) {
			conn.setAutoCommit(false); /* This is done so that all the Batch
										 * statements execute in a single
										 * transaction and no operation in the
										 * batch is committed individually. */
			s.addBatch("ALTER TABLE viaggi.vacanza ADD PRIMARY KEY(codice);");
			s.addBatch("ALTER TABLE viaggi.cliente DROP PRIMARY KEY, ADD PRIMARY KEY(codicefiscale);");
			s.addBatch("ALTER TABLE viaggi.cliente CHANGE COLUMN codice codice INT DEFAULT NULL;");
			s.addBatch("ALTER TABLE viaggi.villeggiante ADD CONSTRAINT FK__villeggiante__vacanze FOREIGN KEY (vacanza) REFERENCES viaggi.vacanza(codice) ON DELETE CASCADE ON UPDATE RESTRICT;");
			s.addBatch("ALTER TABLE viaggi.villeggiante ADD CONSTRAINT FK__villeggiante__cliente FOREIGN KEY (codicefiscale) REFERENCES viaggi.cliente(codicefiscale) ON DELETE CASCADE ON UPDATE RESTRICT;");
			s.executeBatch();
			conn.commit();
			conn.setAutoCommit(true);
		} catch( SQLException e) {
			conn.rollback();
		}
	}

	public void dml() throws SQLException {

		String[] queries = {	"SELECT c.codicefiscale, COUNT(vil.codicefiscale) AS 'Vacanze Effettuate' FROM cliente c JOIN villeggiante vil ON c.codicefiscale = vil.codicefiscale GROUP BY c.codicefiscale;",
								"SELECT va.codice, descrizione FROM vacanza va LEFT JOIN villeggiante vi ON va.codice = vi.vacanza WHERE vi.vacanza IS NULL",
								"SELECT c.nome, c.cognome, c.recapitotel, COUNT(v.codicefiscale) AS 'Villeggiature Effettuate' FROM cliente c LEFT JOIN villeggiante v ON c.codicefiscale = v.codicefiscale GROUP BY v.codicefiscale",
								"SELECT c.codicefiscale, COUNT(vil.codicefiscale) AS 'Vacanze Effettuate' FROM cliente c JOIN villeggiante vil ON c.codicefiscale = vil.codicefiscale GROUP BY c.codicefiscale;",
								"SELECT va.codice, descrizione FROM vacanza va LEFT JOIN villeggiante vi ON va.codice = vi.vacanza WHERE vi.vacanza IS NULL;",
								"SELECT vac.codice, vac.descrizione, c.nome, c.cognome, c.recapitotel, c.email FROM (cliente c JOIN villeggiante vil ON c.codicefiscale = vil.codicefiscale) JOIN vacanza vac ON vil.vacanza = vac.codice;" };
	
		this.forEachQuery(queries, ";", "--------------------------------------------------------------------------");
	}

	public void populate() throws SQLException {

		try( Statement s = conn.createStatement()) {
			conn.setAutoCommit(false); // for further info read line 88.
			String str = "";
			try( BufferedReader br = new BufferedReader(new FileReader($POPULATE_FILE_PATH))) {
				while( ((str = br.readLine()) != null)) {
					if( str.charAt(0) != '#') s.addBatch(str);
				}
			} catch( FileNotFoundException e) {
				System.out.println($POPULATE_FILE_PATH + " not found.");
			} catch( IOException e) {
				System.out.println("IOException while reading file " + $POPULATE_FILE_PATH);
				System.exit(74);
			}
			s.executeBatch();
			conn.commit();
			conn.setAutoCommit(true);
		} catch( SQLException e) {
			conn.rollback();
		}
	}
}
