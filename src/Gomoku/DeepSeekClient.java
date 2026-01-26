package Gomoku;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;

public class DeepSeekClient {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private final OkHttpClient client;
    private final String apiKey;

    public DeepSeekClient(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(35, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
    //准备并发送请求
    public JSONObject queryAI(String prompt) throws IOException {
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept-Encoding", "identity")  // 防止 gzip 阻塞
                .post(RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        buildRequestJson(prompt).toString()
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("API Error: " + response.code() + " " + response.message());

            ResponseBody body = response.body();
            if (body == null) throw new IOException("Empty response");

            String result = body.string();
            System.out.println("DeepSeek 返回内容：" + result);
            return new JSONObject(result);
        }
    }
    //准备请求内容
    private JSONObject buildRequestJson(String prompt) {
        return new JSONObject()
                .put("model", "deepseek-chat")
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "user").put("content", prompt))
                );
    }
}
