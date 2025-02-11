package com.akto.dao;

import java.util.ArrayList;
import java.util.List;

import com.akto.dao.context.Context;
import com.akto.dto.Log;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Indexes;

import org.bson.Document;

public class LogsDao extends AccountsContextDao<Log> {

    public static final LogsDao instance = new LogsDao();
    public void initIndices() {
        boolean exists = false;
        String dbName = Context.accountId.get()+"";
        MongoDatabase db = clients[0].getDatabase(dbName);
        for (String col: db.listCollectionNames()){
            if (getCollName().equalsIgnoreCase(col)){
                exists = true;
                break;
            }
        };

        if (!exists) {
            db.createCollection(getCollName(), new CreateCollectionOptions().capped(true).maxDocuments(100_000).sizeInBytes(100_000_000));
        }
        
        MongoCursor<Document> cursor = db.getCollection(getCollName()).listIndexes().cursor();
        List<Document> indices = new ArrayList<>();

        while (cursor.hasNext()) {
            indices.add(cursor.next());
        }

        if (indices.size() == 0) {
            instance.getMCollection().createIndex(Indexes.descending(Log.TIMESTAMP));
        }
    }

    private LogsDao() {
        initIndices();
    }

    @Override
    public String getCollName() {
        return "logs";
    }

    @Override
    public Class<Log> getClassT() {
        return Log.class;
    }
    
}
