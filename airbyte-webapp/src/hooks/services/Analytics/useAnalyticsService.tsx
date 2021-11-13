import React, { useContext, useEffect, useMemo } from "react";
import { useMap } from "react-use";

import { AnalyticsService } from "core/analytics/AnalyticsService";

type AnalyticsContext = Record<string, unknown>;

export type AnalyticsServiceProviderValue = {
  analyticsContext: AnalyticsContext;
  setContext: (ctx: AnalyticsContext) => void;
  addContextProps: (props: AnalyticsContext) => void;
  removeContextProps: (props: string[]) => void;
  service: AnalyticsService;
};

const analyticsServiceContext = React.createContext<AnalyticsServiceProviderValue | null>(
  null
);

function AnalyticsServiceProvider({
  children,
  version,
  initialContext = {},
}: {
  children: React.ReactNode;
  version?: string;
  initialContext?: AnalyticsContext;
}) {
  const [analyticsContext, { set, setAll, remove }] = useMap(initialContext);

  const analyticsService: AnalyticsService = useMemo(
    () => new AnalyticsService(analyticsContext, version),
    [version, analyticsContext]
  );

  const handleAddContextProps = (props: AnalyticsContext) => {
    Object.entries(props).forEach((value) => set(...value));
  };

  const handleRemoveContextProps = (props: string[]) => props.forEach(remove);

  return (
    <analyticsServiceContext.Provider
      value={{
        analyticsContext,
        setContext: setAll,
        addContextProps: handleAddContextProps,
        removeContextProps: handleRemoveContextProps,
        service: analyticsService,
      }}
    >
      {children}
    </analyticsServiceContext.Provider>
  );
}

export const useAnalyticsService = (): AnalyticsService => {
  const analyticsService = useAnalytics();

  return analyticsService.service;
};

export const useAnalytics = (): AnalyticsServiceProviderValue => {
  const analyticsContext = useContext(analyticsServiceContext);

  if (!analyticsContext) {
    throw new Error(
      "analyticsContext must be used within a AnalyticsServiceProvider."
    );
  }

  return analyticsContext;
};

export const useRegisterAnalyticsValues = (
  props?: AnalyticsContext | null
): void => {
  const { addContextProps, removeContextProps } = useAnalytics();

  useEffect(() => {
    if (props) {
      addContextProps(props);

      return () => removeContextProps(Object.keys(props));
    }

    return;
  }, [props]);
};

export default React.memo(AnalyticsServiceProvider);
