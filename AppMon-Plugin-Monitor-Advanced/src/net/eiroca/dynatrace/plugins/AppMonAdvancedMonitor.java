/**
 *
 * Copyright (C) 2001-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - AGPL >= 3.0
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 **/
package net.eiroca.dynatrace.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.Status;
import net.eiroca.library.core.Helper;
import net.eiroca.library.diagnostics.CommandException;
import net.eiroca.library.diagnostics.IServerMonitor;
import net.eiroca.library.diagnostics.actions.LocalCommandAction;
import net.eiroca.library.diagnostics.actions.SSHCommandAction;
import net.eiroca.library.diagnostics.actions.WebServiceAction;
import net.eiroca.library.diagnostics.monitors.ApacheServerMonitor;
import net.eiroca.library.diagnostics.monitors.DatabaseMonitor;
import net.eiroca.library.diagnostics.monitors.ElasticSearchMonitor;
import net.eiroca.library.diagnostics.monitors.FLUMEServerMonitor;
import net.eiroca.library.diagnostics.monitors.GraphiteMonitor;
import net.eiroca.library.diagnostics.monitors.OracleMonitor;
import net.eiroca.library.diagnostics.monitors.PostgreSQLMonitor;
import net.eiroca.library.diagnostics.monitors.RedisMonitor;
import net.eiroca.library.diagnostics.monitors.DataPowerMonitor;
import net.eiroca.library.diagnostics.monitors.TCPServerMonitor;
import net.eiroca.library.diagnostics.monitors.WebServerMonitor;
import net.eiroca.library.diagnostics.monitors.eSysAdmServerMonitor;
import net.eiroca.library.dynatrace.sdk.AbstractMonitorPlugin;
import net.eiroca.library.dynatrace.sdk.DynatraceContext;
import net.eiroca.library.dynatrace.sdk.DynatracePlugin;
import net.eiroca.library.metrics.MetricGroup;

public class AppMonAdvancedMonitor extends AbstractMonitorPlugin {

  static {
    AbstractMonitorPlugin.name = "Advanced Server Monitor";
  }

  private static final String CONFIG_MONITORTYPE = "monitorType";

  private static Map<String, Class<?>> monitors = new HashMap<>();
  static {
    monitors.put("Apache Web Server", ApacheServerMonitor.class);
    monitors.put("Database Server", DatabaseMonitor.class);
    monitors.put("DataPower Server", DataPowerMonitor.class);
    monitors.put("ElasticSearch Server", ElasticSearchMonitor.class);
    monitors.put("eSysAdm Server", eSysAdmServerMonitor.class);
    monitors.put("FLUME Server", FLUMEServerMonitor.class);
    monitors.put("Graphite Server", GraphiteMonitor.class);
    monitors.put("Local command", LocalCommandAction.class);
    monitors.put("Oracle Database Server", OracleMonitor.class);
    monitors.put("PostgreSQL Server", PostgreSQLMonitor.class);
    monitors.put("Redis Server", RedisMonitor.class);
    monitors.put("SSH command", SSHCommandAction.class);
    monitors.put("TCP Server", TCPServerMonitor.class);
    monitors.put("Web Server", WebServerMonitor.class);
    monitors.put("WebService", WebServiceAction.class);
  }

  @Override
  public Status monitor(final DynatraceContext<MonitorEnvironment> context, final String host) throws Exception {
    Status status = new Status(Status.StatusCode.Success);
    final String monitorType = context.getConfigString(CONFIG_MONITORTYPE);
    final Class<?> monitorClass = monitors.get(monitorType);
    if (monitorClass == null) {
      status = new Status(Status.StatusCode.ErrorInternalConfigurationProblem, "Unknown type: " + monitorType);
    }
    else {
      final IServerMonitor monitor = (IServerMonitor)monitorClass.newInstance();
      try {
        final List<MetricGroup> groups = new ArrayList<>();
        monitor.setup(context);
        monitor.check(host);
        monitor.loadMetricGroup(groups);
        DynatracePlugin.publishMeasures(context, groups);
      }
      catch (final CommandException err) {
        status = fromException(err);
      }
      finally {
        Helper.close(monitor);
      }
    }
    context.info(context.getRunner(), " end: ", status.getShortMessage());
    return status;
  }

}