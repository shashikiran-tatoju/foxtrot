package com.flipkart.foxtrot.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.foxtrot.common.ActionResponse;
import com.flipkart.foxtrot.common.Table;
import com.flipkart.foxtrot.common.TableFieldMapping;
import com.flipkart.foxtrot.core.exception.FoxtrotExceptions;
import com.flipkart.foxtrot.core.querystore.QueryExecutor;
import com.flipkart.foxtrot.core.querystore.QueryStore;
import com.flipkart.foxtrot.core.table.TableMetadataManager;
import com.flipkart.foxtrot.gandalf.access.AccessService;
import com.flipkart.foxtrot.sql.query.FqlActionQuery;
import com.flipkart.foxtrot.sql.query.FqlDescribeTable;
import com.flipkart.foxtrot.sql.query.FqlShowTablesQuery;
import com.flipkart.foxtrot.sql.responseprocessors.Flattener;
import com.flipkart.foxtrot.sql.responseprocessors.FlatteningUtils;
import com.flipkart.foxtrot.sql.responseprocessors.model.FlatRepresentation;
import com.google.common.collect.Lists;
import com.phonepe.gandalf.models.user.UserDetails;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FqlEngine {

    private static final Logger logger = LoggerFactory.getLogger(FqlEngine.class.getSimpleName());

    private TableMetadataManager tableMetadataManager;
    private QueryStore queryStore;
    private QueryExecutor queryExecutor;
    private ObjectMapper mapper;

    public FqlEngine(TableMetadataManager tableMetadataManager, QueryStore queryStore, QueryExecutor queryExecutor,
            ObjectMapper mapper) {
        this.tableMetadataManager = tableMetadataManager;
        this.queryStore = queryStore;
        this.queryExecutor = queryExecutor;
        this.mapper = mapper;
    }

    public FlatRepresentation parse(final String fql, UserDetails userDetails, AccessService accessService)
            throws JsonProcessingException {
        QueryTranslator translator = new QueryTranslator();
        FqlQuery query = translator.translate(fql);
        FlatRepresentation response = new QueryProcessor(tableMetadataManager, queryStore, queryExecutor, mapper,
                userDetails, accessService).process(query);
        String prettyResponse = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
        logger.debug("Flat Response: {}", prettyResponse);
        return response;
    }

    private static final class QueryProcessor implements FqlQueryVisitor {

        private TableMetadataManager tableMetadataManager;
        private QueryStore queryStore;
        private QueryExecutor queryExecutor;
        private ObjectMapper mapper;
        private UserDetails userDetails;
        private AccessService accessService;

        private FlatRepresentation result;

        private QueryProcessor(TableMetadataManager tableMetadataManager, QueryStore queryStore,
                QueryExecutor queryExecutor, ObjectMapper mapper, UserDetails userDetails,
                AccessService accessService) {
            this.tableMetadataManager = tableMetadataManager;
            this.queryStore = queryStore;
            this.queryExecutor = queryExecutor;
            this.mapper = mapper;
            this.userDetails = userDetails;
            this.accessService = accessService;
        }

        public FlatRepresentation process(FqlQuery query) {
            query.receive(this);
            return result;
        }

        @Override
        public void visit(FqlDescribeTable fqlDescribeTable) {
            TableFieldMapping fieldMetaData = queryStore.getFieldMappings(fqlDescribeTable.getTableName());
            result = FlatteningUtils.genericMultiRowParse(mapper.valueToTree(fieldMetaData.getMappings()),
                    Lists.newArrayList("field", "type"), "field");
        }

        @Override
        public void visit(FqlShowTablesQuery fqlShowTablesQuery) {
            List<Table> tables = tableMetadataManager.get();
            result = FlatteningUtils.genericMultiRowParse(mapper.valueToTree(tables), Lists.newArrayList("name", "ttl"),
                    "name");
        }

        @Override
        public void visit(FqlActionQuery fqlActionQuery) {
            try {
                if (!accessService.hasAccess(fqlActionQuery.getActionRequest(), userDetails)) {
                    throw FoxtrotExceptions.createAuthorizationException(fqlActionQuery.getActionRequest(),
                            new Exception("User not Authorised"));
                }
            } catch (Exception e) {
                throw FoxtrotExceptions.createAuthorizationException(fqlActionQuery.getActionRequest(), e);
            }
            try {
                String query = mapper.writeValueAsString(fqlActionQuery.getActionRequest());
                logger.info("Generated query: {}", query);
            } catch (JsonProcessingException e) {
                //ignoring the exception as it is coming while logging.
                logger.error("Error in serializing action request.", e);
            }
            ActionResponse actionResponse = queryExecutor.execute(fqlActionQuery.getActionRequest(),
                    userDetails.getEmail());
            Flattener flattener = new Flattener(mapper, fqlActionQuery.getActionRequest(),
                    fqlActionQuery.getSelectedFields());
            actionResponse.accept(flattener);
            result = flattener.getFlatRepresentation();
        }

    }
}
