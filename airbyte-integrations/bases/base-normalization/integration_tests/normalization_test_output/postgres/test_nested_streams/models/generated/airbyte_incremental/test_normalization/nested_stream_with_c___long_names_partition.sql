{{ config(
    indexes = [{'columns':['_airbyte_emitted_at'],'type':'hash'}],
    unique_key = '_airbyte_ab_id',
    schema = "test_normalization",
    tags = [ "nested" ]
) }}
-- Final base SQL model
select
    _airbyte_nested_stre__nto_long_names_hashid,
    double_array_data,
    {{ adapter.quote('DATA') }},
    _airbyte_ab_id,
    _airbyte_emitted_at,
    {{ current_timestamp() }} as _airbyte_normalized_at,
    _airbyte_partition_hashid
from {{ ref('nested_stream_with_c___long_names_partition_ab3') }}
-- partition at nested_stream_with_complex_columns_resulting_into_long_names/partition from {{ ref('nested_stream_with_c__lting_into_long_names_scd') }}
where 1 = 1
{{ incremental_clause('_airbyte_emitted_at') }}

