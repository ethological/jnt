package war.jnt.git;

import com.google.gson.annotations.SerializedName;

public class Git {
    private static class Commit {
        @SerializedName("sha")
        private String sha;

        public String getSha() {
            return sha;
        }
    }

    public static String getHash(String authorization) {
//        HttpURLConnection con = null;
//        try {
//            String apiUrl = "https://api.github.com/repos/JavaNativeTranspiler/jnt/commits";
//
//            URL url = new URL(apiUrl);
//            con = (HttpURLConnection) url.openConnection();
//            con.setRequestMethod("GET");
//            con.setRequestProperty("Authorization", "Bearer " + authorization);
//            con.setRequestProperty("Accept", "application/vnd.github+json");
//
//            int status = con.getResponseCode();
//            if (status != 200) {
//                throw new RuntimeException("Failed to get commits: HTTP " + status);
//            }
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = in.readLine()) != null) {
//                response.append(line);
//            }
//            in.close();
//
//            Gson gson = new Gson();
//            List<Commit> commits = gson.fromJson(response.toString(), new TypeToken<List<Commit>>(){}.getType());
//
//            if (commits == null || commits.isEmpty()) {
//                return "UNKNOWN";
//            }
//
//            String hash = commits.getFirst().getSha();
//            String shortSha = hash.substring(0, Math.min(hash.length(), 7));
//            return shortSha;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "ERROR: " + e.getMessage();
//        } finally {
//            if (con != null) {
//                con.disconnect();
//            }
//        }
        return "UNKNOWN";
    }
}
