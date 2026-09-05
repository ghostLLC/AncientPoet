import java.sql.*;
import org.flywaydb.core.Flyway;

/** Creates an old-schema fixture in the verification script's disposable database. */
class PrepareUpgrade {
    public static void main(String[] args) throws Exception {
        var env=System.getenv();
        var url="jdbc:postgresql://"+env.get("POSTGRES_HOST")+":"+env.get("POSTGRES_PORT")+"/"+env.get("POSTGRES_DB");
        Flyway.configure().dataSource(url,env.get("POSTGRES_USER"),env.get("POSTGRES_PASSWORD"))
            .locations("classpath:db/migration").target("3").load().migrate();
        try(var db=DriverManager.getConnection(url,env.get("POSTGRES_USER"),env.get("POSTGRES_PASSWORD"));
            var statement=db.createStatement()) {
            statement.execute("INSERT INTO users(phone,nickname) VALUES ('13800000999','upgrade fixture')");
            statement.execute("INSERT INTO conversations(user_id,poet_id,dynasty_id,storyline_current_year) "+
                "SELECT u.id,p.id,p.dynasty_id,p.death_year+10 FROM users u CROSS JOIN poets p WHERE u.phone='13800000999' ORDER BY p.id LIMIT 1");
            statement.execute("INSERT INTO messages(conversation_id,sender_type,content_text,is_delivered) "+
                "SELECT id,'user','legacy letter preserved',false FROM conversations");
            statement.execute("INSERT INTO conversation_summaries(conversation_id,summary_text,covers_up_to) "+
                "SELECT conversation_id,'legacy summary',id FROM messages");
        }
    }
}
