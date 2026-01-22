package com.xgen.testing.mongot.index.query.collectors;

import com.xgen.mongot.index.query.collectors.TermStatsDefinition;
import com.xgen.mongot.util.Check;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TermStatsDefinitionBuilder {

  private Optional<List<String>> fields = Optional.empty();

  public TermStatsDefinitionBuilder fields(List<String> fields) {
    this.fields = Optional.of(new ArrayList<>(fields));
    return this;
  }

  public TermStatsDefinitionBuilder addField(String field) {
    if (this.fields.isEmpty()) {
      this.fields = Optional.of(new ArrayList<>());
    }
    this.fields.get().add(field);
    return this;
  }

  public TermStatsDefinition build() {
    Check.isPresent(this.fields, "fields");
    return new TermStatsDefinition(this.fields.get());
  }
}
