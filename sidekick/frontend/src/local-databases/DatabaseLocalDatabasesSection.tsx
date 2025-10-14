import { useState } from "react";
import { t } from "ttag";

import {
  DatabaseInfoSection,
  DatabaseInfoSectionDivider,
} from "metabase/admin/databases/components/DatabaseInfoSection";
import { Flex, Icon, UnstyledButton } from "metabase/ui";
import type { Database } from "metabase-types/api";

import { LocalDatabasesList } from "./LocalDatabasesList";

export const DatabaseLocalDatabasesSection = ({
  database,
}: {
  database: Database;
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (database.engine !== "sidekick") {
    return null;
  }

  return (
    <DatabaseInfoSection
      name={t`Local Databases`}
      description={t`The local databases paired with this Sidekick DB instance.`}
    >
      <Flex gap="md" justify={"right"}>
        <UnstyledButton onClick={() => setIsExpanded(!isExpanded)} px="xs">
          <Icon name={isExpanded ? "chevronup" : "chevrondown"} />
        </UnstyledButton>
      </Flex>

      {isExpanded && (
        <>
          <DatabaseInfoSectionDivider />
          <LocalDatabasesList sidekickDatabaseId={database.id} />
        </>
      )}
    </DatabaseInfoSection>
  );
};
