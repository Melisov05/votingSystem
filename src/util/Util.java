package util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import entity.Candidate;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class Util {

    public static List<Candidate> readCandidatesFromJson(String jsonFilePath) {
        try (FileReader reader = new FileReader(jsonFilePath)) {
            Type candidateListType = new TypeToken<List<Candidate>>() {}.getType();

            return new Gson().fromJson(reader, candidateListType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
