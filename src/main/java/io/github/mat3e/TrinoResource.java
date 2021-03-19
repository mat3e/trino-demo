package io.github.mat3e;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Path("/api/trino")
public class TrinoResource {
    private static final String FIND_COUNT = "select count(t.description) as tasks, mu.mail as mail, gu.name as name, gu.surname as surname from mysql.todos.tasks t left join mongo.userdb.users mu on t.assigned_user = mu.id left join sheets.default.users gu on gu.number = cast(mu.id as varchar) group by mu.mail, gu.name, gu.surname";
    private static final String FIND_OVERVIEW = "select t.description as task, g.description as task_group, mu.mail as mail, gu.name as name, gu.surname as surname from mysql.todos.tasks t left join mysql.todos.task_groups g on t.task_group_id = g.id left join mongo.userdb.users mu on t.assigned_user = mu.id left join sheets.default.users gu on gu.number = cast(mu.id as varchar)";

    private final DataSource dataSource;

    @Inject
    public TrinoResource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public QueryAndList<CountedResponse> count() {
        return QueryAndList.from(dataSource, FIND_COUNT, CountedResponse::from);
    }

    @GET
    @Path("/overview")
    @Produces(MediaType.APPLICATION_JSON)
    public QueryAndList<GeneralResponse> overview() {
        return QueryAndList.from(dataSource, FIND_OVERVIEW, GeneralResponse::from);
    }

    static record QueryAndList<T>(String query, List<T> data) {
        static <T> QueryAndList<T> from(DataSource dataSource, String query, Function<ResultSet, T> mapper) {
            return new QueryAndList<>(
                    query,
                    getResponseList(dataSource, query, mapper)
            );
        }

        private static <T> List<T> getResponseList(DataSource dataSource, String query, Function<ResultSet, T> mapper) {
            List<T> result = new ArrayList<>();
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(query)) {
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(mapper.apply(resultSet));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }

    static record CountedResponse(int tasks, String mail, String name, String surname) {
        static CountedResponse from(ResultSet resultSet) {
            try {
                return new CountedResponse(
                        resultSet.getInt("tasks"),
                        resultSet.getString("mail"),
                        resultSet.getString("name"),
                        resultSet.getString("surname")
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static record GeneralResponse(String task, String taskGroup, String mail, String name, String surname) {
        static GeneralResponse from(ResultSet resultSet) {
            try {
                return new GeneralResponse(
                        resultSet.getString("task"),
                        resultSet.getString("task_group"),
                        resultSet.getString("mail"),
                        resultSet.getString("name"),
                        resultSet.getString("surname")
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}