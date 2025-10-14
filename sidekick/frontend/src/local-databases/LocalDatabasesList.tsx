import cx from "classnames";
import { useMemo } from "react";
import { t } from "ttag";

import { DatabaseConnectionHealthInfo } from "metabase/admin/databases/components/DatabaseConnectionHealthInfo";
import { useDeleteDatabaseMutation, useListDatabasesQuery } from "metabase/api";
import { LoadingAndErrorWrapper } from "metabase/common/components/LoadingAndErrorWrapper";
import AdminS from "metabase/css/admin.module.css";
import CS from "metabase/css/core/index.css";
import { useSelector } from "metabase/lib/redux";
import RunButtonWithTooltip from "metabase/query_builder/components/RunButtonWithTooltip";
import { getUserIsAdmin } from "metabase/selectors/user";
import { Box, Button, Flex, Text } from "metabase/ui";

export const LocalDatabasesList = ({
  sidekickDatabaseId,
}: {
  sidekickDatabaseId: number;
}) => {
  const isAdmin = useSelector(getUserIsAdmin);

  const localDbsReq = useListDatabasesQuery({
    router_database_id: sidekickDatabaseId,
  });

  const localDatabases = useMemo(
    () => localDbsReq.data?.data ?? [],
    [localDbsReq],
  );

  const [deleteDatabase] = useDeleteDatabaseMutation();

  return (
    <>
      <Flex justify={"right"}>
        <RunButtonWithTooltip
          isRunning={localDbsReq.isFetching}
          onRun={() => localDbsReq.refetch()}
          tooltip={t`Refresh`}
        />
      </Flex>
      <LoadingAndErrorWrapper
        loading={localDbsReq.isLoading}
        error={localDbsReq.error}
      >
        <Box component="section">
          {localDatabases.length === 0 ? (
            <Text
              ta="center"
              mt="3.5rem"
              mb="3.5rem"
            >{t`No local databases`}</Text>
          ) : (
            <table className={cx(AdminS.ContentTable, CS.borderBottom)}>
              <thead>
                <tr>
                  <th>{t`Status`}</th>
                  <th>{t`Database Name`}</th>
                  <th>{t`Local`}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {localDatabases.map((db) => (
                  <tr key={db.id}>
                    <td>
                      <DatabaseConnectionHealthInfo
                        databaseId={db.id}
                        displayText="tooltip"
                      />
                    </td>
                    <td>{db.name}</td>
                    <td>
                      {db.details && typeof db.details.local === "string"
                        ? db.details.local
                        : undefined}
                    </td>
                    <td>
                      {isAdmin && (
                        <Button
                          variant="filled"
                          color="danger"
                          onClick={() => deleteDatabase(db.id)}
                        >{t`Delete`}</Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Box>
      </LoadingAndErrorWrapper>
    </>
  );
};
