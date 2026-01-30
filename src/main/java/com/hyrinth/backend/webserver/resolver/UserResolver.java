package com.hyrinth.backend.webserver.resolver;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.storage.account.AccountRecord;
import com.hyrinth.backend.entities.HyrinthUser;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class UserResolver {

    public static JSONObject getUser(String id) {
        if(id == null) return null;
        Optional<AccountRecord> user = Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findById(id);
        if (user.isPresent()) {
            JSONObject json = new JSONObject();
            json.put("id", user.get().id());
            json.put("email", user.get().email());
            json.put("username", user.get().username());
            json.put("profilePicture", user.get().profilePicture());
            json.put("isHidden", user.get().isHidden());
            json.put("projects", user.get().projects());
            json.put("teams", user.get().teams());
            json.put("createdAt", user.get().createdAt());
            json.put("updatedAt", user.get().updatedAt());
            return json;
        } else {
            return null;
        }
    }

    public static JSONObject getAuthenticatedUser(String token) {
        JSONObject json = getUser(Main.getHyrinthBackend().getAccountSessionService().findAccountId(token).orElse(null));
        if(json != null) {
            String id = json.getString("id");
            Optional<AccountRecord> user = Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findById(id);
            if (user.isPresent()) {
                json.put("passwordHash", user.get().passwordHash());
                json.put("permissions", user.get().permissions());
                json.put("isAdmin", user.get().isAdmin());
                json.put("usedToken", token);
                return json;
            }
        }
        return null;
    }

    public static HyrinthUser findByToken(String token) {
        String id = Main.getHyrinthBackend().getAccountSessionService().findAccountId(token).orElse(null);
        if(id != null) {
            Optional<AccountRecord> user = Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findById(id);
            if(user.isPresent()) {
                return new HyrinthUser(user.get());
            }
        }
        return null;
    }

    public static HyrinthUser findById(String id) {
        return Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findById(id).map(HyrinthUser::new).orElse(null);
    }

    public static JSONObject getUsers(int limit, int offset) {
        if(limit>100) {
            limit = 100;
        }

        JSONObject response = new JSONObject();
        response.put("limit", limit);
        response.put("offset", offset);

        JSONArray users = new JSONArray();
        List<AccountRecord> accounts = Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findPage(limit, offset);
        AtomicInteger i = new AtomicInteger();
        accounts.forEach(user -> {
            users.add(getUser(user.id()));
            i.getAndIncrement();
        });

        response.put("matches", accounts.size());
        response.put("total_hits",-1);
        response.put("hits", users);
        return response;
    }
}
