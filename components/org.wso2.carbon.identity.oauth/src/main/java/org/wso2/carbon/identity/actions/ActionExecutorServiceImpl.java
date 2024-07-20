/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.actions.exception.ActionExecutionException;
import org.wso2.carbon.identity.actions.model.ActionExecutionRequest;
import org.wso2.carbon.identity.actions.model.ActionExecutionResponse;
import org.wso2.carbon.identity.actions.model.AllowedOperation;
import org.wso2.carbon.identity.actions.model.PerformableOperation;
import org.wso2.carbon.identity.actions.util.OperationComparator;
import org.wso2.carbon.identity.oauth2.token.AccessTokenIssuer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ActionInvocationService.
 */

public class ActionExecutorServiceImpl implements ActionExecutorService {

    private static final Log log = LogFactory.getLog(ActionExecutorServiceImpl.class);

    private static final ActionExecutorServiceImpl instance = new ActionExecutorServiceImpl();
    private final APIClient apiClient;

    private ActionExecutorServiceImpl() {

        apiClient = new APIClient();
    }

    public static ActionExecutorServiceImpl getInstance() {

        return instance;
    }

    public ActionExecutionResponse execute(ActionType actionType, Map<String, Object> eventContext) throws
            ActionExecutionException {

        ActionExecutionRequest request =
                ActionExecutionRequestBuilderFactory.buildActionInvocationRequest(actionType, eventContext);

        ActionExecutionResponse response =
                apiClient.callAPI("https://mpd07d9c71841be2961c.free.beeceptor.com/anymock/pre-issue-access-token", request);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            log.info("=== Action Response: \n" + jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<PerformableOperation> allowedPerformableOperations = validatePerformableOperations(request, response);
        response.setOperations(allowedPerformableOperations);

        ActionExecutionResponseProcessorFactory.getActionInvocationResponseProcessor(actionType)
                .processResponse(actionType, eventContext, request.getEvent(), response);

        return response;
    }

    // implement a method to validate if the paths of PerformableOperations in ActionInvocationResponse matches with paths of allowedOperations on ActionInvocationRequest.
    private List<PerformableOperation> validatePerformableOperations(ActionExecutionRequest request,
                                                                     ActionExecutionResponse response) {

        List<AllowedOperation> allowedOperations = request.getAllowedOperations();

        List<PerformableOperation> allowedPerformableOperations = response.getOperations().stream()
                .filter(performableOperation -> allowedOperations.stream()
                        .anyMatch(allowedOperation -> OperationComparator.compare(allowedOperation,
                                performableOperation)))
                .collect(Collectors.toList());

        response.getOperations().forEach(operation -> {
            if (allowedPerformableOperations.contains(operation)) {
                log.info("==== Operation " + operation.getOp() + " with path " + operation.getPath() + " is allowed.");
            } else {
                log.info("==== Operation " + operation.getOp() + " with path " + operation.getPath() +
                        " is not allowed.");
            }
        });

        return allowedPerformableOperations;
    }
}
