package org.wycliffeassociates.trConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

public class BookParser {

    JSONArray arrayOfBooks = new JSONArray();

    public BookParser()
    {
        try {
            InputStream fis = getClass().getClassLoader().getResourceAsStream("assets/books.json");
            String json = ReadInputStream(fis);
            arrayOfBooks = new JSONArray(json);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String GetAnthology(String slug)
    {
        String ant = null;

        try {
            for (int i = 0; i < arrayOfBooks.length(); i++) {
                JSONObject jsonBook = arrayOfBooks.getJSONObject(i);
                String bookSlug = jsonBook.getString("slug");

                if(slug.equals(bookSlug))
                {
                    ant = jsonBook.getString("anth");
                }
            }
        }
        catch(JSONException e)
        {
            System.out.printf(e.getMessage());
        }

        return ant;
    }

    public int GetBookNumber(String slug)
    {
        int bn = -1;

        try {
            for (int i = 0; i < arrayOfBooks.length(); i++) {
                JSONObject jsonBook = arrayOfBooks.getJSONObject(i);
                String bookSlug = jsonBook.getString("slug");
                if (slug.equals(bookSlug)) {
                    bn = jsonBook.getInt("num");
                }
            }
        }
        catch(JSONException e)
        {
            System.out.printf(e.getMessage());
        }

        return bn;
    }

    private String ReadInputStream(InputStream fis)
    {
        try (Scanner scanner = new Scanner(fis)) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
