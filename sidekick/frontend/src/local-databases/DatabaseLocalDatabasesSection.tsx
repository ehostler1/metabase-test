import { t } from "ttag";

import { DatabaseInfoSection } from "metabase/admin/databases/components/DatabaseInfoSection";
import type { Database } from "metabase-types/api";

import { LocalDatabasesList } from "./LocalDatabasesList";

export const DatabaseLocalDatabasesSection = ({
  database,
}: {
  database: Database;
}) => {
  if (database.engine !== "sidekick") {
    return null;
  }

  return (
    <DatabaseInfoSection
      name={t`Local Databases`}
      description={t`The local databases paired with this Sidekick DB instance.`}
    >
      <LocalDatabasesList sidekickDatabaseId={database.id} />
    </DatabaseInfoSection>
  );
};
