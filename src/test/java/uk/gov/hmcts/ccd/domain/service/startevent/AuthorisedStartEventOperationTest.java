package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class AuthorisedStartEventOperationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String UID = "23";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String EVENT_TRIGGER_ID = "updateEvent";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;

    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Map<String, JsonNode> EMPTY_MAP = Maps.newHashMap();


    @Mock
    private StartEventOperation classifiedStartEventOperation;

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private AuthorisedStartEventOperation authorisedStartEventOperation;
    @Mock
    private UserRepository userRepository;

    private CaseDetails classifiedCaseDetails;
    private JsonNode authorisedCaseDetailsNode;
    private JsonNode authorisedCaseDetailsClassificationNode;
    private JsonNode classifiedCaseDetailsNode;
    private JsonNode classifiedCaseDetailsClassificationNode;
    private StartEventTrigger classifiedStartEvent;
    private final CaseType caseType = new CaseType();
    private final List<CaseField> caseFields = Lists.newArrayList();
    private final Set<String> userRoles = Sets.newHashSet(CASEWORKER_DIVORCE,
                                                          CASEWORKER_PROBATE_LOA1,
                                                          CASEWORKER_PROBATE_LOA3);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        classifiedCaseDetailsNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedCaseDetailsNode).put("dataTestField", "dataTestValue");
        ((ObjectNode) classifiedCaseDetailsNode).put("dataTestField2", "dataTestValue2");
        classifiedCaseDetailsClassificationNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedCaseDetailsClassificationNode).put("classificationTestField",
                                                                   "classificationTestValue");
        ((ObjectNode) classifiedCaseDetailsClassificationNode).put("classificationTestField2",
                                                                   "classificationTestValue2");

        authorisedCaseDetailsNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedCaseDetailsNode).put("dataTestField", "dataTestValue");
        authorisedCaseDetailsClassificationNode = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedCaseDetailsClassificationNode).put("classificationTestField",
                                                                   "classificationTestValue");

        classifiedCaseDetails = new CaseDetails();
        classifiedCaseDetails.setData(MAPPER.convertValue(classifiedCaseDetailsNode, STRING_JSON_MAP));
        classifiedCaseDetails.setDataClassification(MAPPER.convertValue(classifiedCaseDetailsClassificationNode,
                                                                        STRING_JSON_MAP));
        classifiedStartEvent = new StartEventTrigger();
        classifiedStartEvent.setCaseDetails(classifiedCaseDetails);

        authorisedStartEventOperation = new AuthorisedStartEventOperation(classifiedStartEventOperation,
                                                                          caseDefinitionRepository,
                                                                          accessControlService,
                                                                          userRepository);
        caseType.setCaseFields(caseFields);
        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(userRepository.getUserRoles()).thenReturn(userRoles);
        when(accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ)).thenReturn(true);
        when(accessControlService.filterCaseFieldsByAccess(eq(classifiedCaseDetailsNode),
                                                               eq(caseFields),
                                                               eq(userRoles),
                                                               eq(CAN_READ))).thenReturn(authorisedCaseDetailsNode);
        when(accessControlService.filterCaseFieldsByAccess(eq(classifiedCaseDetailsClassificationNode),
                                                               eq(caseFields),
                                                               eq(userRoles),
                                                               eq(CAN_READ))).thenReturn(
            authorisedCaseDetailsClassificationNode);
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @BeforeEach
        void setUp() {
            doReturn(classifiedStartEvent).when(classifiedStartEventOperation).triggerStartForCaseType(UID,
                                                                                                          JURISDICTION_ID,
                                                                                                          CASE_TYPE_ID,
                                                                                                          EVENT_TRIGGER_ID,
                                                                                                          IGNORE_WARNING);
            when(accessControlService.canAccessCaseTypeWithCriteria(caseType,
                                                                    userRoles,
                                                                    CAN_CREATE)).thenReturn(true);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            final StartEventTrigger output = authorisedStartEventOperation.triggerStartForCaseType(UID,
                                                                                                   JURISDICTION_ID,
                                                                                                   CASE_TYPE_ID,
                                                                                                   EVENT_TRIGGER_ID,
                                                                                                   IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> verify(classifiedStartEventOperation).triggerStartForCaseType(UID,
                                                                                    JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    EVENT_TRIGGER_ID,
                                                                                    IGNORE_WARNING)
            );
        }

        @Test
        @DisplayName("should filter out data when no case type read access")
        void shouldFilterOutDataWhenNoCaseTypeReadAccess() {

            when(accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ)).thenReturn(false);

            final StartEventTrigger output = authorisedStartEventOperation.triggerStartForCaseType(UID,
                                                                                                   JURISDICTION_ID,
                                                                                                   CASE_TYPE_ID,
                                                                                                   EVENT_TRIGGER_ID,
                                                                                                   IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails().getData(), is(EMPTY_MAP)),
                () -> assertThat(output.getCaseDetails().getDataClassification(), is(EMPTY_MAP)),
                () -> verify(classifiedStartEventOperation).triggerStartForCaseType(UID,
                                                                                    JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    EVENT_TRIGGER_ID,
                                                                                    IGNORE_WARNING)
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @BeforeEach
        void setUp() {
            doReturn(classifiedStartEvent).when(classifiedStartEventOperation).triggerStartForCase(UID,
                                                                                                      JURISDICTION_ID,
                                                                                                      CASE_TYPE_ID,
                                                                                                      CASE_REFERENCE,
                                                                                                      EVENT_TRIGGER_ID,
                                                                                                      IGNORE_WARNING);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            StartEventTrigger output = authorisedStartEventOperation.triggerStartForCase(UID,
                                                                                         JURISDICTION_ID,
                                                                                         CASE_TYPE_ID,
                                                                                         CASE_REFERENCE,
                                                                                         EVENT_TRIGGER_ID,
                                                                                         IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> verify(classifiedStartEventOperation).triggerStartForCase(UID,
                                                                                JURISDICTION_ID,
                                                                                CASE_TYPE_ID,
                                                                                CASE_REFERENCE,
                                                                                EVENT_TRIGGER_ID,
                                                                                IGNORE_WARNING)
            );
        }

        @Test
        @DisplayName("should return event trigger as is when case details null")
        void shouldReturnEventTriggerWhenCaseDetailsNull() {
            classifiedStartEvent.setCaseDetails(null);

            final StartEventTrigger output = authorisedStartEventOperation.triggerStartForCase(UID,
                                                                                               JURISDICTION_ID,
                                                                                               CASE_TYPE_ID,
                                                                                               CASE_REFERENCE,
                                                                                               EVENT_TRIGGER_ID,
                                                                                               IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), is(nullValue()))
            );
        }

        @Test
        @DisplayName("should return event trigger with classified case details when not empty")
        void shouldReturnEventTriggerWithClassifiedCaseDetails() {

            final StartEventTrigger output = authorisedStartEventOperation.triggerStartForCase(UID,
                                                                                               JURISDICTION_ID,
                                                                                               CASE_TYPE_ID,
                                                                                               CASE_REFERENCE,
                                                                                               EVENT_TRIGGER_ID,
                                                                                               IGNORE_WARNING);

            InOrder inOrder = inOrder(caseDefinitionRepository,
                                      userRepository,
                                      classifiedStartEventOperation,
                                      accessControlService);
            assertAll(
                () -> assertThat(output, sameInstance(classifiedStartEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedCaseDetails)),
                () -> assertThat(output.getCaseDetails().getData(),
                                 is(equalTo(MAPPER.convertValue(authorisedCaseDetailsNode, STRING_JSON_MAP)))),
                () -> assertThat(output.getCaseDetails().getDataClassification(),
                                 is(equalTo(MAPPER.convertValue(authorisedCaseDetailsClassificationNode,
                                                                STRING_JSON_MAP)))),
                () -> inOrder.verify(classifiedStartEventOperation).triggerStartForCase(UID,
                                                                                        JURISDICTION_ID,
                                                                                        CASE_TYPE_ID,
                                                                                        CASE_REFERENCE,
                                                                                        EVENT_TRIGGER_ID,
                                                                                        IGNORE_WARNING),
                () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> inOrder.verify(userRepository).getUserRoles(),
                () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                                                                                         eq(userRoles),
                                                                                         eq(CAN_READ)),
                () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedCaseDetailsNode),
                                                                                        eq(caseFields),
                                                                                        eq(userRoles),
                                                                                        eq(CAN_READ)),
                () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedCaseDetailsClassificationNode),
                                                                                        eq(caseFields),
                                                                                        eq(userRoles),
                                                                                        eq(CAN_READ))
            );
        }

        @Test
        @DisplayName("should fail if case type not found")
        void shouldFailIfNoCaseTypeFound() {

            doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

            assertThrows(ValidationException.class, () -> authorisedStartEventOperation.triggerStartForCase(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            CASE_REFERENCE,
                                                                                                            EVENT_TRIGGER_ID,
                                                                                                            IGNORE_WARNING));
        }

        @Test
        @DisplayName("should fail if user roles not found")
        void shouldFailIfNoUserRolesFound() {

            doReturn(null).when(userRepository).getUserRoles();

            assertThrows(ValidationException.class, () -> authorisedStartEventOperation.triggerStartForCase(UID,
                                                                                                            JURISDICTION_ID,
                                                                                                            CASE_TYPE_ID,
                                                                                                            CASE_REFERENCE,
                                                                                                            EVENT_TRIGGER_ID,
                                                                                                            IGNORE_WARNING));
        }
    }

}
