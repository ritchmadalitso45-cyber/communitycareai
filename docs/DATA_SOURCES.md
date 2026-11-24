# Data sources and provenance

Runtime data comes exclusively from the configured, owned DHIS2 tracker API, scoped to the worker's assigned organisation, configured program and stage. The six currently verified records are engineering tests, not a clinical dataset. Unit-test fixtures are synthetic and are not inserted as model training data by the analysis endpoint.

WHO's Data Quality Assurance toolkit provides conceptual reporting-quality guidance:
- https://www.who.int/data/data-collection-tools/health-service-data/data-quality-assurance-dqa
- https://www.who.int/publications/i/item/9789240063938

WHO guidance is a reference, not model training data, endorsement or evidence that this implementation is validated. No identifiable Clinage AI reference was supplied or verified, and no dataset or functionality is attributed to it. No external clinical dataset is used for this nonclinical model.
