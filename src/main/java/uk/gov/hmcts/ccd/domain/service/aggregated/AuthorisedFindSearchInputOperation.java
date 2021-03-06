package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

@Service
@Qualifier(AuthorisedFindSearchInputOperation.QUALIFIER)
public class AuthorisedFindSearchInputOperation implements FindSearchInputOperation {

    public static final String QUALIFIER = "authorised";
    private final FindSearchInputOperation findSearchInputOperation;
    private final GetCaseTypesOperation getCaseTypesOperation;

    public AuthorisedFindSearchInputOperation(@Qualifier(ClassifiedFindSearchInputOperation.QUALIFIER) final FindSearchInputOperation findSearchInputOperation,
                                              @Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER) final GetCaseTypesOperation getCaseTypesOperation) {
        this.findSearchInputOperation = findSearchInputOperation;
        this.getCaseTypesOperation = getCaseTypesOperation;
    }

    public List<SearchInput> execute(final String jurisdictionId, final String caseTypeId, Predicate<AccessControlList> access) {
        Optional<CaseType> caseType = this.getCaseTypesOperation.execute(jurisdictionId, access)
            .stream()
            .filter(ct -> ct.getId().equalsIgnoreCase(caseTypeId))
            .findFirst();

        if(!caseType.isPresent()){
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }

        return findSearchInputOperation.execute(jurisdictionId, caseTypeId, access)
            .stream()
            .filter(searchInput -> caseType.get().getCaseFields()
                .stream()
                .anyMatch(caseField -> caseField.getId().equalsIgnoreCase(searchInput.getField().getId())))
            .collect(toList());
    }
}
