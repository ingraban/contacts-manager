package name.saak.contactmanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO für die Antwort der Feiertage-API.
 * Die API gibt ein Objekt mit dem Datum und optionalen Hinweis zurück.
 */
public class HolidayApiResponse {

    @JsonProperty("datum")
    private String datum;

    @JsonProperty("hinweis")
    private String hinweis;

    public HolidayApiResponse() {
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public String getHinweis() {
        return hinweis;
    }

    public void setHinweis(String hinweis) {
        this.hinweis = hinweis;
    }

    @Override
    public String toString() {
        return "HolidayApiResponse{" +
                "datum='" + datum + '\'' +
                ", hinweis='" + hinweis + '\'' +
                '}';
    }
}
