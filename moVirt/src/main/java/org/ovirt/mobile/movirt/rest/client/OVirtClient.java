package org.ovirt.mobile.movirt.rest.client;

import android.content.Context;
import android.support.annotation.NonNull;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.rest.spring.annotations.RestService;
import org.androidannotations.rest.spring.api.RestClientHeaders;
import org.androidannotations.rest.spring.api.RestClientRootUrl;
import org.androidannotations.rest.spring.api.RestClientSupport;
import org.ovirt.mobile.movirt.MoVirtApp;
import org.ovirt.mobile.movirt.auth.properties.AccountProperty;
import org.ovirt.mobile.movirt.auth.properties.manager.AccountPropertiesManager;
import org.ovirt.mobile.movirt.auth.properties.property.version.Version;
import org.ovirt.mobile.movirt.auth.properties.property.version.support.VersionSupport;
import org.ovirt.mobile.movirt.model.Cluster;
import org.ovirt.mobile.movirt.model.Console;
import org.ovirt.mobile.movirt.model.DataCenter;
import org.ovirt.mobile.movirt.model.Disk;
import org.ovirt.mobile.movirt.model.DiskAttachment;
import org.ovirt.mobile.movirt.model.Event;
import org.ovirt.mobile.movirt.model.Host;
import org.ovirt.mobile.movirt.model.Nic;
import org.ovirt.mobile.movirt.model.Snapshot;
import org.ovirt.mobile.movirt.model.StorageDomain;
import org.ovirt.mobile.movirt.model.Vm;
import org.ovirt.mobile.movirt.provider.OVirtContract;
import org.ovirt.mobile.movirt.rest.Request;
import org.ovirt.mobile.movirt.rest.RequestHandler;
import org.ovirt.mobile.movirt.rest.Response;
import org.ovirt.mobile.movirt.rest.RestEntityWrapper;
import org.ovirt.mobile.movirt.rest.RestEntityWrapperList;
import org.ovirt.mobile.movirt.rest.client.requestfactory.OvirtSimpleClientHttpRequestFactory;
import org.ovirt.mobile.movirt.rest.dto.Action;
import org.ovirt.mobile.movirt.rest.dto.Events;
import org.ovirt.mobile.movirt.rest.dto.SnapshotAction;
import org.ovirt.mobile.movirt.util.message.MessageHelper;
import org.ovirt.mobile.movirt.util.preferences.SettingsKey;
import org.ovirt.mobile.movirt.util.preferences.SharedPreferencesHelper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ovirt.mobile.movirt.rest.RestHelper.setAcceptEncodingHeaderAndFactory;
import static org.ovirt.mobile.movirt.rest.RestHelper.setFilterHeader;
import static org.ovirt.mobile.movirt.rest.RestHelper.setVersionHeader;
import static org.ovirt.mobile.movirt.rest.RestHelper.setupAuth;

@EBean(scope = EBean.Scope.Singleton)
public class OVirtClient {
    private static final String TAG = OVirtClient.class.getSimpleName();

    @RestService
    OVirtRestClient restClient;

    @Bean
    RequestHandler requestHandler;

    @Bean
    OvirtSimpleClientHttpRequestFactory requestFactory;

    @RootContext
    Context context;

    @Bean
    AccountPropertiesManager propertiesManager;

    @App
    MoVirtApp app;

    @Bean
    SharedPreferencesHelper sharedPreferencesHelper;

    @Bean
    MessageHelper messageHelper;

    private Version version;

    @AfterInject
    public void init() {
        setAcceptEncodingHeaderAndFactory(restClient, requestFactory);

        propertiesManager.notifyAndRegisterListener(new AccountProperty.VersionListener() {
            @Override
            public void onPropertyChange(Version newVersion) {
                setVersionHeader(restClient, newVersion);
                setupAuth(restClient, newVersion);
                version = newVersion;
            }
        });

        propertiesManager.notifyAndRegisterListener(new AccountProperty.ApiUrlListener() {
            @Override
            public void onPropertyChange(String apiUrl) {
                restClient.setRootUrl(apiUrl);
            }
        });

        propertiesManager.notifyAndRegisterListener(new AccountProperty.HasAdminPermissionsListener() {
            @Override
            public void onPropertyChange(Boolean hasAdminPermissions) {
                setFilterHeader(restClient, hasAdminPermissions);
            }
        });
    }

    public void startVm(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.startVm(new Action(), vmId);
                return null;
            }
        }, response);
    }

    public void stopVm(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.stopVm(new Action(), vmId);
                return null;
            }
        }, response);
    }

    public void rebootVm(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.rebootVm(new Action(), vmId);
                return null;
            }
        }, response);
    }

    public void migrateVmToHost(final String vmId, final String hostId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                Action action = version.isV3Api() ? new org.ovirt.mobile.movirt.rest.dto.v3.ActionMigrate(hostId) :
                        new org.ovirt.mobile.movirt.rest.dto.v4.ActionMigrate(hostId);
                restClient.migrateVmToHost(action, vmId);
                return null;
            }
        }, response);
    }

    public void migrateVmToDefaultHost(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.migrateVmToHost(new Action(), vmId);
                return null;
            }
        }, response);
    }

    public void cancelMigration(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.cancelMigration(new Action(), vmId);
                return null;
            }
        }, response);
    }

    @NonNull
    public Request<Vm> getVmRequest(final String vmId) {
        return new RestClientRequest<Vm>() {
            @Override
            public Vm fire() {
                org.ovirt.mobile.movirt.rest.dto.Vm vm = version.isV3Api() ? restClient.getVmV3(vmId) : restClient.getVmV4(vmId);
                return vm.toEntity();
            }
        };
    }

    public void getVm(final String vmId, Response<Vm> response) {
        requestHandler.fireRestRequest(getVmRequest(vmId), response);
    }

    public void activateHost(final String hostId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.activateHost(new Action(), hostId);
                return null;
            }
        }, response);
    }

    public void dectivateHost(final String hostId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.deactivateHost(new Action(), hostId);
                return null;
            }
        }, response);
    }

    public void deleteSnapshot(final String vmId, final String snapshotId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.deleteSnapshot(vmId, snapshotId);
                return null;
            }
        }, response);
    }

    public void restoreSnapshot(final SnapshotAction snapshotAction, final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                String snapshotId = snapshotAction.snapshot.id;
                SnapshotAction restAction = new SnapshotAction(snapshotAction.restore_memory);

                restClient.restoreSnapshot(restAction, vmId, snapshotId);
                return null;
            }
        }, response);
    }

    public void previewSnapshot(final SnapshotAction snapshotAction, final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                if (version.isV3Api()) {
                    restClient.previewSnapshotV3(snapshotAction, vmId);
                } else {
                    restClient.previewSnapshotV4(snapshotAction, vmId);
                }
                return null;
            }
        }, response);
    }

    public void createSnapshot(final org.ovirt.mobile.movirt.rest.dto.Snapshot snapshot, final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                restClient.createSnapshot(snapshot, vmId);
                return null;
            }
        }, response);
    }

    public void commitSnapshot(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                if (version.isV3Api()) {
                    restClient.commitSnapshotV3(new Action(), vmId);
                } else {
                    restClient.commitSnapshotV4(new Action(), vmId);
                }
                return null;
            }
        }, response);
    }

    public void undoSnapshot(final String vmId, Response<Void> response) {
        requestHandler.fireRestRequest(new RestClientRequest<Void>() {
            @Override
            public Void fire() {
                if (version.isV3Api()) {
                    restClient.undoSnapshotV3(new Action(), vmId);
                } else {
                    restClient.undoSnapshotV4(new Action(), vmId);
                }
                return null;
            }
        }, response);
    }

    @NonNull
    public Request<Host> getHostRequest(final String hostId) {
        return new RestClientRequest<Host>() {
            @Override
            public Host fire() {
                org.ovirt.mobile.movirt.rest.dto.Host wrapper = version.isV3Api() ?
                        restClient.getHostV3(hostId) : restClient.getHostV4(hostId);
                return wrapper.toEntity();
            }
        };
    }

    public void getHost(final String hostId, Response<Host> response) {
        requestHandler.fireRestRequest(getHostRequest(hostId), response);
    }

    @NonNull
    public Request<StorageDomain> getStorageDomainRequest(final String storageDomainId) {
        return new RestClientRequest<StorageDomain>() {
            @Override
            public StorageDomain fire() {
                org.ovirt.mobile.movirt.rest.dto.StorageDomain wrapper = version.isV3Api() ?
                        restClient.getStorageDomainV3(storageDomainId) :
                        restClient.getStorageDomainV4(storageDomainId);
                return wrapper.toEntity();
            }
        };
    }

    public void getStorageDomain(final String storageDomainId, Response<StorageDomain> response) {
        requestHandler.fireRestRequest(getStorageDomainRequest(storageDomainId), response);
    }

    // currently not used
    public Request<Disk> getDiskRequest(final String vmId, final String id) {
        return getDiskRequest(vmId, null, id);
    }

    @NonNull
    public Request<Disk> getDiskRequest(final String vmId, final String snapshotId, final String id) {
        final boolean isSnapshotEmbedded = snapshotId != null;

        return new RestClientRequest<Disk>() {
            @Override
            public Disk fire() {
                org.ovirt.mobile.movirt.rest.dto.Disk wrapper;
                Disk entity;

                if (isSnapshotEmbedded) {
                    if (version.isV3Api()) {
                        wrapper = restClient.getDiskV3(vmId, snapshotId, id);
                    } else {
                        wrapper = restClient.getDiskV4(vmId, snapshotId, id);
                    }
                    entity = wrapper.toEntity();
                    setVmId(entity, vmId);
                } else {
                    if (version.isV3Api()) {
                        wrapper = restClient.getDiskV3(vmId, id);
                    } else {
                        wrapper = restClient.getDiskV4(id);
                    }
                    entity = wrapper.toEntity();
                }

                return entity;
            }
        };
    }

    public Request<List<DiskAttachment>> getDisksAttachmentsRequest(final String vmId) {

        return new RestClientRequest<List<DiskAttachment>>() {
            @Override
            public List<DiskAttachment> fire() {
                VersionSupport.DISK_ATTACHMENTS.throwIfNotSupported(version);

                return mapToEntities(restClient.getDisksAttachmentsV4(vmId));
            }
        };
    }

    public Request<List<Disk>> getDisksRequest(final String vmId, final String snapshotId) {
        final boolean downloadAll = vmId == null;
        final boolean isSnapshotEmbedded = snapshotId != null;

        return new RestClientRequest<List<Disk>>() {
            @Override
            public List<Disk> fire() {
                RestEntityWrapperList<? extends org.ovirt.mobile.movirt.rest.dto.Disk> wrappers;
                List<Disk> entities;

                if (downloadAll) {
                    if (version.isV3Api()) {
                        wrappers = restClient.getDisksV3();
                    } else {
                        wrappers = restClient.getDisksV4();
                    }
                    entities = mapToEntities(wrappers);
                } else if (isSnapshotEmbedded) {
                    if (version.isV3Api()) {
                        wrappers = restClient.getDisksV3(vmId, snapshotId);
                    } else {
                        wrappers = restClient.getDisksV4(vmId, snapshotId);
                    }
                    entities = mapToEntities(wrappers);
                    setVmId(entities, vmId);
                } else {
                    VersionSupport.VM_DISKS.throwIfNotSupported(version);

                    if (version.isV3Api()) {
                        wrappers = restClient.getDisksV3(vmId);
                    } else {
                        wrappers = restClient.getDisksV4(vmId);
                    }

                    entities = mapToEntities(wrappers);
                }

                return entities;
            }
        };
    }

    public void getClusters(Response<List<Cluster>> response) {
        requestHandler.fireRestRequest(new RestClientRequest<List<Cluster>>() {
            @Override
            public List<Cluster> fire() {
                if (version.isV3Api()) {
                    return mapToEntities(restClient.getClustersV3());
                }
                return mapToEntities(restClient.getClustersV4());
            }
        }, response);
    }

    public void getDataCenters(Response<List<DataCenter>> response) {
        requestHandler.fireRestRequest(new RestClientRequest<List<DataCenter>>() {
            @Override
            public List<DataCenter> fire() {
                if (version.isV3Api()) {
                    return mapToEntities(restClient.getDataCentersV3());
                }
                return mapToEntities(restClient.getDataCentersV4());
            }
        }, response);
    }

    public Request<Nic> getNicRequest(final String vmId, final String id) {
        return getNicRequest(vmId, null, id);
    }

    @NonNull
    public Request<Nic> getNicRequest(final String vmId, final String snapshotId, final String id) {
        return new RestClientRequest<Nic>() {
            @Override
            public Nic fire() {
                org.ovirt.mobile.movirt.rest.dto.Nic wrapper;
                Nic entity;

                if (snapshotId == null) {
                    if (version.isV3Api()) {
                        wrapper = restClient.getNicV3(vmId, id);
                    } else {
                        wrapper = restClient.getNicV4(vmId, id);
                    }
                    entity = wrapper.toEntity();
                    setVmId(entity, vmId);
                } else {
                    if (version.isV3Api()) {
                        wrapper = restClient.getNicV3(vmId, snapshotId, id);
                    } else {
                        wrapper = restClient.getNicV4(vmId, snapshotId, id);
                    }
                    entity = wrapper.toEntity();
                }

                return entity;
            }
        };
    }

    public Request<List<Nic>> getNicsRequest(final String vmId) {
        return getNicsRequest(vmId, null);
    }

    public Request<List<Nic>> getNicsRequest(final String vmId, final String snapshotId) {
        return new RestClientRequest<List<Nic>>() {
            @Override
            public List<Nic> fire() {
                RestEntityWrapperList<? extends org.ovirt.mobile.movirt.rest.dto.Nic> wrappers;
                List<Nic> entities;

                if (snapshotId == null) {
                    if (version.isV3Api()) {
                        wrappers = restClient.getNicsV3(vmId);
                    } else {
                        wrappers = restClient.getNicsV4(vmId);
                    }
                    entities = mapToEntities(wrappers);
                    setVmId(entities, vmId);
                } else {
                    if (version.isV3Api()) {
                        wrappers = restClient.getNicsV3(vmId, snapshotId);
                    } else {
                        wrappers = restClient.getNicsV4(vmId, snapshotId);
                    }
                    entities = mapToEntities(wrappers);
                }

                return entities;
            }
        };
    }

    public Request<List<Host>> getHostsRequest() {
        return new RestClientRequest<List<Host>>() {
            @Override
            public List<Host> fire() {
                if (version.isV3Api()) {
                    return mapToEntities(restClient.getHostsV3());
                }
                return mapToEntities(restClient.getHostsV4());
            }
        };
    }

    public Request<List<Vm>> getVmsRequest() {

        return new RestClientRequest<List<Vm>>() {
            @Override
            public List<Vm> fire() {
                RestEntityWrapperList<? extends org.ovirt.mobile.movirt.rest.dto.Vm> wrappers;

                if (propertiesManager.hasAdminPermissions()) {
                    int maxVms = sharedPreferencesHelper.getMaxVms();
                    String query = sharedPreferencesHelper.getStringPref(SettingsKey.VMS_SEARCH_QUERY);
                    if (StringUtils.isEmpty(query)) {
                        wrappers = version.isV3Api() ? restClient.getVmsV3(maxVms) :
                                restClient.getVmsV4(maxVms);
                    } else {
                        wrappers = version.isV3Api() ? restClient.getVmsV3(query, maxVms) :
                                restClient.getVmsV4(query, maxVms);
                    }
                } else {
                    wrappers = version.isV3Api() ? restClient.getVmsV3(-1) :
                            restClient.getVmsV4(-1);
                }

                return mapToEntities(wrappers);
            }
        };
    }

    public Request<List<StorageDomain>> getStorageDomainsRequest() {
        return new RestClientRequest<List<StorageDomain>>() {
            @Override
            public List<StorageDomain> fire() {
                if (version.isV3Api()) {
                    return mapToEntities(restClient.getStorageDomainsV3());
                }
                return mapToEntities(restClient.getStorageDomainsV4());
            }
        };
    }

    public Request<List<Snapshot>> getSnapshotsRequest(final String vmId) {
        return new RestClientRequest<List<Snapshot>>() {
            @Override
            public List<Snapshot> fire() {
                RestEntityWrapperList<? extends org.ovirt.mobile.movirt.rest.dto.Snapshot> wrappers;
                List<Snapshot> entities;

                if (version.isV3Api()) {
                    wrappers = restClient.getSnapshotsV3(vmId);
                } else {
                    wrappers = restClient.getSnapshotsV4(vmId);
                }

                entities = mapToEntities(wrappers);
                setVmId(entities, vmId); // Active VM Snapshot doesn't include this

                return entities;
            }
        };
    }

    public Request<Snapshot> getSnapshotRequest(final String vmId, final String snapshotId) {
        return new RestClientRequest<Snapshot>() {
            @Override
            public Snapshot fire() {
                org.ovirt.mobile.movirt.rest.dto.Snapshot wrapper;
                Snapshot entity;

                if (version.isV3Api()) {
                    wrapper = restClient.getSnapshotV3(vmId, snapshotId);
                } else {
                    wrapper = restClient.getSnapshotV4(vmId, snapshotId);
                }

                entity = wrapper.toEntity();
                setVmId(entity, vmId);

                return entity;
            }
        };
    }

    public Request<List<Console>> getConsolesRequest(final String vmId) {
        return new RestClientRequest<List<Console>>() {
            @Override
            public List<Console> fire() {
                return mapToEntities(restClient.getConsoles(vmId));
            }
        };
    }

    public void getEventsSince(final int lastEventId, Response<List<Event>> response) {
        requestHandler.fireRestRequest(new RestClientRequest<List<Event>>() {
            @Override
            public List<Event> fire() {
                Events loadedEvents = null;

                if (propertiesManager.hasAdminPermissions()) {
                    int maxEventsStored = sharedPreferencesHelper.getMaxEvents();
                    String query = sharedPreferencesHelper.getStringPref(SettingsKey.EVENTS_SEARCH_QUERY);
                    if (!"".equals(query)) {
                        loadedEvents = restClient.getEventsSince(Integer.toString(lastEventId), query, maxEventsStored);
                    } else {
                        loadedEvents = restClient.getEventsSince(Integer.toString(lastEventId), maxEventsStored);
                    }
                } else {
                    loadedEvents = restClient.getEventsSince(Integer.toString(lastEventId), -1);
                }

                if (loadedEvents == null) {
                    return Collections.emptyList();
                }

                return mapToEntities(loadedEvents, new WrapPredicate<org.ovirt.mobile.movirt.rest.dto.Event>() {
                    @Override
                    public boolean toWrap(org.ovirt.mobile.movirt.rest.dto.Event entity) {
                        return entity.id > lastEventId;
                    }
                });
            }
        }, response);
    }

    private <E, U extends RestEntityWrapper<E>> List<E> mapToEntities(RestEntityWrapperList<U> wrappersList) {
        return mapToEntities(wrappersList, null);
    }

    private <E, U extends RestEntityWrapper<E>> List<E> mapToEntities(RestEntityWrapperList<U> wrappersList, WrapPredicate<U> predicate) {
        if (wrappersList == null) {
            return Collections.emptyList();
        }

        List<U> wrappers = wrappersList.getList();

        if (wrappers == null) {
            return Collections.emptyList();
        }

        List<E> entities = new ArrayList<>();
        for (U rest : wrappers) {
            try {
                if (predicate == null || predicate.toWrap(rest)) {
                    entities.add(rest.toEntity());
                }
            } catch (Exception e) {
                // showing only as a toast since this problem may persist and we don't want to flood the user with messages like this as dialogs...
                messageHelper.showToast("Error parsing rest response, ignoring: " + rest.toString() + " error: " + e.getMessage());
            }
        }
        return entities;
    }

    private <E extends OVirtContract.HasVm> void setVmId(E entity, String vmId) {
        if (entity != null && !StringUtils.isEmpty(vmId)) {
            entity.setVmId(vmId);
        }
    }

    private <E extends OVirtContract.HasVm> void setVmId(List<E> entities, String vmId) {
        if (entities != null && !StringUtils.isEmpty(vmId)) {
            for (E entity : entities) {
                entity.setVmId(vmId);
            }
        }
    }

    private interface WrapPredicate<E> {
        boolean toWrap(E entity);
    }

    private abstract class RestClientRequest<T> implements Request<T> {
        @Override
        @SuppressWarnings("unchecked")
        public <U extends RestClientRootUrl & RestClientHeaders & RestClientSupport> U getRestClient() {
            return (U) restClient;
        }
    }
}
