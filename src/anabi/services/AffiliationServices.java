package anabi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import anabi.models.Affiliation;
import anabi.models.Author;
import anabi.models.Record;
import anabi.utilities.ConnectionDB;
import anabi.utilities.InitServices;

public class AffiliationServices {

	private Record keyRecord;
	private Affiliation affiliation;
	private Integer codAffiliation = 0;
	private Integer countAffiliation = 0;
	private AuthorServices authorServi;
	private List<Affiliation> listAffiliation;
	private List<Integer> listCodAuthor;


	private InitServices iniServices;
	private ConnectionDB connDB;
	private String sql ;

	public AffiliationServices(){
		listAffiliation = new ArrayList<Affiliation>();
		listCodAuthor = new ArrayList<Integer>();
		iniServices  = iniServices.getInstances();


	}

	/** Extraer la afiliacion del record y adicionar a lista de afiliacion. 
	 * 
	 * @param record
	 */
	public void setListAffiliation(HashMap<String, String> record, Record key) {

		this.keyRecord = key;

		String nameAffiliation;
		String cityAffiliation;
		String countryAffiliation;

		String line = record.get("C1");

		try {
			BufferedReader br = new BufferedReader(new StringReader(line));


			while ((line = br.readLine()) != null) {

				String[] dataCOne;
				List<String> authorsListByAffiliation = new ArrayList<>();

				if (line.length() > 1) {
					
					if ( !(line.contains("[")) ){
						line = line.substring(0,0) + "]"+line.substring(0,1) + line.substring(1);
					}

					if (line.contains("]")) {
						
						// Extract authors
						dataCOne = line.split("]");
						String authorsData = dataCOne[0].trim();
						if (authorsData.equals("")) {
							System.out.println("Author data empty");
						} else {
							authorsListByAffiliation = extractAuthorsNames(authorsData);
						}
						
						
						// Extract affiliation
						String[] affiliationData = new String[dataCOne.length];
						affiliationData = dataCOne[1].split(",");

						if ( affiliationData.length > 2) {

							nameAffiliation = affiliationData[0];
							cityAffiliation = affiliationData[affiliationData.length - 2];
							countryAffiliation = affiliationData[affiliationData.length - 1];
						} else {
							affiliationData = dataCOne[1].split(" ");
							nameAffiliation = affiliationData[0];
							cityAffiliation = affiliationData[affiliationData.length - 2];
							countryAffiliation = affiliationData[affiliationData.length - 1];
						}

						if (listAffiliation.size() >= 1){
							affiliation = findAffiliationByName(nameAffiliation, cityAffiliation);
						}

						// Adiciona los autores a la filiacion encontrada
						if ( affiliation != null ) {

							Integer tempCodAffiliation = affiliation.getCodAffiliation();

							List<Integer> listTempCodAuthors = getCodAuthorsList(tempCodAffiliation);
							deleteRelationAffiliationAuthor(tempCodAffiliation);

							authorServi = iniServices.getAuthorServices();
							listCodAuthor = authorServi.buildCodAuthorsList(authorsListByAffiliation);
							listTempCodAuthors.addAll(listCodAuthor);

							addAffiliationAndAuthor(tempCodAffiliation, listTempCodAuthors);
							//affiliation.setListCodAuthor(listTempCodAuthors);


							//							List<Record> listTempRecords = affiliation.getListRecord();
							//							listTempRecords.add(keyRecord);
							//							affiliation.setListRecord(listTempRecords);


						} else {
							// Create a new affiliation
							codAffiliation +=1 ;

							affiliation = new Affiliation(codAffiliation, nameAffiliation, cityAffiliation,countryAffiliation, keyRecord);
							authorServi = iniServices.getAuthorServices();
							listCodAuthor = authorServi.buildCodAuthorsList(authorsListByAffiliation);

							affiliation.setListCodAuthor(listCodAuthor);


							listAffiliation.add(affiliation);
							addAffiliation(codAffiliation, nameAffiliation, cityAffiliation, countryAffiliation);
							addAffiliationAndAuthor(codAffiliation, listCodAuthor);
						}
					} 
				}

			}



		}catch (NullPointerException npe) {
			System.out.println("Valor de linea: " + line);
		} catch (IOException ex) {
			System.out.println("Exception io");
		}

	}



	/**
	 * Return a names authors list. 
	 * @param aDataLine
	 * @return
	 */
	public List<String> extractAuthorsNames (String aDataLine) {

		List<String> result = new ArrayList<String>();

		if ( aDataLine.length() > 0 && aDataLine.contains(";")) {
			result = Arrays.asList(aDataLine.substring(1).split(";"));
		} else {
			if (aDataLine.substring(1).trim().equals("")) {
				System.out.println("linea vacias");
			} else {
				result.add(aDataLine.substring(1));
			}
			
		}

		return result;

	}


	
	/**
	 * Return the affiliation name
	 * @return
	 */
	public String extractAffiliationName (String aDataLine) {
		String result = "";
		
		return result;
	}




	public List<Affiliation> getAffiliationsListAll() {
		return listAffiliation;
	}



	public Integer countAffiliations() {
		return listAffiliation.size();
	}



	public List<String> getAffiliationsListNames() {
		List<String> listResult = new ArrayList<String>();

		for(Affiliation objAffiliation : listAffiliation){
			listResult.add(objAffiliation.getNameAffiliation());
		}
		return listResult;
	} 

	public List<Integer> getAffiliationsListCods (Record keyrecord){

		List<Integer> result = new ArrayList<Integer>();

		Integer idRow = 0;
		String idDocument = "";

		for (Affiliation objAffiliation : listAffiliation ){

			List<Record> listRecord = objAffiliation.getListRecord();

			for (int i=0; i < listRecord.size(); i++){
				idRow = listRecord.get(i).getIDRecord();
				idDocument = listRecord.get(i).getCodDocument();

				if ( (idRow.equals(keyrecord.getIDRecord())) && (idDocument.equals(keyrecord.getCodDocument())) ) {
					result.add(objAffiliation.getCodAffiliation());
				} 
			}
		}
		return result;
	}



	public Affiliation findByName(String aNameAffiliation){

		aNameAffiliation = aNameAffiliation.trim();
		Affiliation result = null;
		boolean founded = false;

		for ( int i = 0; i < listAffiliation.size() && !founded ; i++ ){
			String nameAffiliation = listAffiliation.get(i).getNameAffiliation().trim();

			if ( nameAffiliation.equals(aNameAffiliation) ){
				result = listAffiliation.get(i);
				founded = true;

			}
		}
		return result;
	}



	public Affiliation findByIdAffiliation(Integer codAffiliation){

		Affiliation result = null;
		boolean founded = false;

		for ( int i = 0; i < listAffiliation.size() && !founded ; i++ ){
			Integer idAffiliation = listAffiliation.get(i).getCodAffiliation();

			if ( idAffiliation == codAffiliation ){
				result = listAffiliation.get(i);
				founded = true;
			}
		}
		return result;
	}



	public Integer getNumberAuthors (Integer codAffiliation){

		int result = 0;
		Affiliation objAffiliation = findByIdAffiliation(codAffiliation);

		if (objAffiliation != null){
			result = objAffiliation.getListCodAuthor().size();	
		}

		return result;
	}



	public List<Author> getListAuthor (Integer codAffiliation){

		List<Author> result = new ArrayList<Author>();
		Affiliation objAffiliation = findByIdAffiliation(codAffiliation);

		if (objAffiliation != null){
			List<Integer> listIdAuthor = objAffiliation.getListCodAuthor();
			result = authorServi.getAuthorsList(listIdAuthor);
		}
		return result;
	} 




	public List<Author> getListAuthor (String nameAffiliation){

		List<Author> result = new ArrayList<Author>();
		nameAffiliation = nameAffiliation.trim();

		Affiliation objAffiliation = findByName(nameAffiliation);

		if (objAffiliation != null){
			List<Integer> listIdAuthor = objAffiliation.getListCodAuthor();
			result = authorServi.getAuthorsList(listIdAuthor);
		}

		return result;
	}	


	public List<Affiliation> getListAffiliation (List<Integer> listCodAffiliations){
		List<Affiliation> result = new ArrayList<Affiliation>();

		for (Integer codAffiliation : listCodAffiliations){
			Affiliation objAffiliation = findByIdAffiliation(codAffiliation);
			result.add(objAffiliation);	
		}
		return result;
	}

	public List<String> getListName(List<Affiliation> listAffiliations){
		List<String> result = new ArrayList<String>();

		for (Affiliation objAffiliation : listAffiliations){
			result.add(objAffiliation.getNameAffiliation());
		}
		return result;
	}


	// Add an affiliation in the table Affiliation
	public void addAffiliation(Integer aCodAffiliation, String aNameAffiliation, String aCity, String aCountryAffiliation) {

		sql = "";
		sql = "INSERT INTO affiliation VALUES ("+aCodAffiliation+",\""+aNameAffiliation+"\",\""+aCity+"\",\""+aCountryAffiliation+"\")";

		connDB = iniServices.getDB();
		connDB.runSql(sql);

	}


	// Inserting the Affiliation's author
	public void  addAffiliationAndAuthor(Integer anIdAffiliation, List<Integer> alistCodsAuthors ) {

		sql = "";

		for (Integer aCodAuthor : alistCodsAuthors){
			sql = "INSERT INTO affiliation_author  VALUES ("+anIdAffiliation+","+aCodAuthor+")";
			connDB.runSql(sql);
		}



	}

	// Delete all affiliations
	public void deleteAllAffiliations (){
		sql = "";
		sql = "DELETE FROM affiliation";
		connDB = iniServices.getDB();
		connDB.runSql(sql);

		//deleteAllAffiliationsAuthor();	
	}


	// Delete an occurrence  of the table affiliations_author
	public void deleteRelationAffiliationAuthor (Integer aCodAffiliation){
		sql = "";
		sql = "DELETE FROM affiliation_author WHERE cod_affiliation="+aCodAffiliation;
		connDB = iniServices.getDB();
		connDB.runSql(sql);

	}



	// Delete a affiliation specific
	public void deleteAffiliation(Integer aIdAffiliation){

		sql = "";
		sql = "DELETE FROM affiliation WHERE cod_affiliation="+aIdAffiliation;
		connDB.runSql(sql);
	}


	// Search a affiliation specific, receive as parameter the name affiliation
	public Affiliation findAffiliationByName (String aNameAffiliation, String aCityAffiliation) {

		affiliation = null;

		if ( aNameAffiliation.contains("'")){
			aNameAffiliation = aNameAffiliation.replace("'"," ").trim();
		}

		if ( aCityAffiliation.contains("'")){
			aCityAffiliation = aCityAffiliation.replace("'"," ").trim();
		}

		sql = "";
		sql = "SELECT * FROM affiliation WHERE name_affiliation='"+aNameAffiliation+"' AND city_affiliation='"+aCityAffiliation+"'";

		ResultSet rs = connDB.runSql(sql);

		try {
			while( rs.next()){
				affiliation = new Affiliation();
				affiliation.setCodAffiliation(rs.getInt("cod_affiliation"));
				affiliation.setNameAffiliation(rs.getString("name_affiliation"));
				affiliation.setCityAffiliation(rs.getString("city_affiliation"));
				affiliation.setCountryAffiliation(rs.getString("country_affiliation"));
				affiliation.setListCodAuthor(listCodAuthor);
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		return affiliation;
	}


	public List<Integer> getCodAuthorsList (Integer aIdAffiliation) {

		List<Integer> result = new ArrayList<Integer>();
		sql = "";
		sql = "SELECT * FROM affiliation_author WHERE cod_affiliation="+aIdAffiliation;

		ResultSet rs = connDB.runSql(sql);

		try {
			while( rs.next()){
				result.add(rs.getInt("cod_author"));
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}


		return result;
	}


}
