package com.example.gps.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.audio.AudioTriggerService;
import com.example.gps.geofence.GeoFenceManager;
import com.example.gps.location.LocationService;
import com.example.gps.location.TrackManager;
import com.example.gps.map.CampusMapManager;
import com.example.gps.util.DistanceUtil;
import com.example.gps.util.RecyclerViewHeightUtil;
import com.example.gps.util.PermissionUtil;
import com.example.gps.map.CoordinateUtils;
import com.example.gps.map.MarkerManager;
import com.example.gps.map.PoiInfoWindow;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.CheckInDto;
import com.example.gps.network.dto.CheckInReq;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.network.dto.ManualPlayReq;
import com.example.gps.network.dto.MultiStopRouteReq;
import com.example.gps.network.dto.RoutePlanDto;
import com.example.gps.network.dto.UserRouteDetailDto;
import com.example.gps.network.dto.UserRouteSummaryDto;
import com.example.gps.network.dto.TriggeredLocationDto;
import com.example.gps.network.dto.UserDto;
import com.example.gps.storage.SearchHistoryStore;
import com.example.gps.storage.SessionManager;
import com.example.gps.ui.adapter.NearbyLocationAdapter;
import com.example.gps.ui.adapter.RouteWaypointAdapter;
import com.example.gps.ui.adapter.SearchHistoryAdapter;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteBusLineItem;
import com.amap.api.services.route.RouteBusWalkItem;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.route.WalkStep;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements AudioTriggerService.Host {

    private static final int REQ_LOCATION = PermissionUtil.REQ_LOCATION;
    private static final long CHECK_INTERVAL_MS = 12000L;
    private static final long AUTO_PLAY_COOLDOWN_MS = 120000L;
    private static final double CAMPUS_INSIDE_RADIUS_METERS = 3200.0;
    private static final float FIRST_FIX_MAX_ACCURACY_METERS = 260f;
    private static final float CONTINUOUS_MAX_ACCURACY_METERS = 160f;
    private static final float LOCATION_JUMP_ACCEPT_ACCURACY_METERS = 40f;
    private static final float LOCATION_JUMP_FILTER_METERS = 1200f;
    private static final long LOCATION_JUMP_FILTER_WINDOW_MS = 15000L;
    private static final int BOTTOM_PANEL_PEEK_DP = 72;
    private static final float BOTTOM_PANEL_MAX_HEIGHT_FRACTION = 0.42f;
    private static final int BOTTOM_PANEL_MAX_HEIGHT_DP = 300;
    private static final int DEFAULT_RADIUS_METERS = 120;
    private static final double CAMPUS_ROUTE_WALK_MPS = 1.25;
    private static final double CAMPUS_ROUTE_DRIVE_MPS = 7.2;
    private static final double CAMPUS_ROUTE_BUS_MPS = 5.0;
    private static final double CAMPUS_ROUTE_TAXI_MPS = 8.3;
    private static final int CAMPUS_ROUTE_MINUTES_BIAS = 1;
    private static final long AMAP_SEARCH_LOCATION_ID_BASE = 900_000_000L;
    private static final long AMAP_CLICKED_POI_LOCATION_ID = AMAP_SEARCH_LOCATION_ID_BASE + 50_000_000L;
    private static final double AMAP_POI_ACCEPT_RADIUS_METERS = 15000.0;
    private static final double ROUTE_LANDMARK_RADIUS_METERS = 120.0;
    private static final double ROUTE_STEP_SPOT_MATCH_METERS = 45.0;
    private static final int ROUTE_WAYPOINT_MAX_COUNT = 40;
    private static final int ROUTE_LANDMARK_MAX_COUNT = 8;
    private static final int ROUTE_LANDMARK_SAMPLE_COUNT = 5;
    private static final String[] ROUTE_LANDMARK_KEYWORDS = {
            "西南大学", "教学楼", "学院", "图书馆", "食堂", "实验室", "运动场"
    };
    private static final Pattern TEACHING_BUILDING_NUMBER_PATTERN =
            Pattern.compile("(\\d{1,3})\\s*(?:号)?\\s*(?:教学楼|教|号楼|楼)?");
    private static final Pattern STRICT_TEACHING_BUILDING_NUMBER_PATTERN =
            Pattern.compile("(?:第)?\\s*(\\d{1,3})\\s*(?:号)?\\s*(?:教学楼|教学|号楼|教楼|教)");
    private static final String ROUTE_START_CURRENT = "__CURRENT__";
    private static final String ROUTE_END_DESTINATION = "__DESTINATION__";
    private static final String ROUTE_END_LOC_PREFIX = "LOC:";

    private CampusMapManager campusMapManager;
    private MarkerManager markerManager;
    private MapView mapView;
    private EditText etSearch;
    private View btnSearch;
    private View searchHistoryPanel;
    private RecyclerView rvSearchHistory;
    private SearchHistoryStore searchHistoryStore;
    private SearchHistoryAdapter searchHistoryAdapter;
    private View locateFloatBall;
    private View btnZoomIn;
    private View btnZoomOut;
    private ImageView btnSettings;
    private View btnLayer;
    private View btnFeedback;
    private TextView tileAutoGuide;
    private TextView tileTrack;
    private TextView tileNavigation;
    private TextView tvLocationName;
    private TextView tvDistance;
    private TextView tvDescription;
    private TextView tvRouteSummary;
    private TextView tvRouteSteps;
    private TextView chipRouteWalk;
    private TextView chipRouteDrive;
    private TextView chipRouteBus;
    private TextView chipRouteTaxi;
    private TextView btnNavHome;
    private TextView btnNavAround;
    private TextView btnNavShare;
    private TextView btnNavMine;
    private View btnManualPlay;
    private View btnRoute;
    private View btnNearestSpots;
    private View nearbySection;
    private View mapProgress;
    private View bottomPanel;
    private View bottomPanelHandle;
    private View bottomNavBar;
    private View layoutRouteDetail;
    private View cardRoutePanel;
    private RecyclerView rvRouteDirections;
    private RecyclerView rvRouteNamedPlaces;
    private RouteWaypointAdapter routeDirectionsAdapter;
    private RouteWaypointAdapter routeNamedPlacesAdapter;
    private List<GeoPoint> lastRoutePolylinePoints = new ArrayList<>();
    private long routeDrawGeneration;
    @Nullable
    private GeoPoint lastRouteStartPoint;
    private TextView tvGuidePoint;
    private Spinner spinnerRouteStart;
    private Spinner spinnerRouteEnd;
    private View btnSwapRoute;

    private final List<String> routeStartOptionIds = new ArrayList<>();
    private final List<String> routeStartOptionLabels = new ArrayList<>();
    private final List<String> routeEndOptionIds = new ArrayList<>();
    private final List<String> routeEndOptionLabels = new ArrayList<>();
    private String selectedRouteStartId = ROUTE_START_CURRENT;
    @Nullable
    private String selectedRouteEndId = ROUTE_END_DESTINATION;

    private SessionManager session;
    private ApiService api;

    private final List<LocationDto> locations = new ArrayList<>();
    private final Map<Long, GeoPoint> locationDisplayPoints = new HashMap<>();
    private final Map<Long, GeoPoint> locationCoordinateOverrides = new HashMap<>();
    private final Set<Long> pendingPoiCoordinateFixes = new HashSet<>();
    private final Map<String, GeoPoint> campusRouteNodes = new LinkedHashMap<>();
    private final Map<String, String> campusRouteNodeLabels = new HashMap<>();
    private final Map<String, List<String>> campusRouteGraph = new HashMap<>();
    private final Map<Long, String> poiRouteNodeHints = new HashMap<>();

    private NearbyLocationAdapter nearbyAdapter;
    private LocationDto selectedLocation;
    private GeoPoint currentGeoPoint;
    private GeoPoint currentRawGeoPoint;
    private Marker userMarker;
    private Polyline routePolyline;
    @Nullable
    private Polyline routePolylineOutline;

    private LocationService locationService;
    private TrackManager trackManager;
    private GeoFenceManager geoFenceManager;
    private AudioTriggerService audioTriggerService;
    private RouteSearch routeSearch;
    private boolean hasAutoCentered = false;
    private boolean defaultMapAnchorApplied = false;

    private boolean autoGuideEnabled = true;
    private boolean trackEnabled = true;
    private boolean geofenceVisible = true;
    private boolean bottomPanelCollapsed = false;
    private float panelCollapsedTranslation = 0f;
    private boolean followMyLocation = false;
    private boolean keepMapOnUserResult = false;
    private float panelTouchDownY = 0f;
    private float panelTouchDownTranslation = 0f;
    private boolean panelDragging = false;
    private RouteMode routeMode = RouteMode.WALK;
    private long routedLocationId = -1L;
    private long pendingRouteLocationId = -1L;
    private RouteMode pendingRouteMode = null;
    private long nextAmapSearchLocationId = AMAP_SEARCH_LOCATION_ID_BASE;
    private int lastAmapPoiSearchErrorCode = AMapException.CODE_AMAP_SUCCESS;
    private long lastLocationUpdateTimeMs = 0L;
    private int lastLocalFenceHitCount = 0;
    private long routeLandmarkSearchToken = 0L;
    private long searchSessionId = 0L;
    private static final int MAX_AMAP_SEARCH_QUERIES = 8;
    /** 手动讲解切换：按景点计数，换景点后从「第一次=开始」重新算。 */
    private long manualGuideLocationId = -1L;

    private long activeSavedRouteId = -1L;

    private ActivityResultLauncher<Intent> spotsIntroLauncher;
    private ActivityResultLauncher<Intent> spotDetailLauncher;

    /** 是否已成功从后端拉取景点（讲解点/起终点选择仅在此为 true 时可用）。 */
    private boolean backendLocationsLoaded = false;
    private boolean suppressRouteSpinnerEvents = false;

    private enum MapCameraMode {
        FREE,
        LOCKED_ON_SELECTION,
        LOCKED_ON_ROUTE
    }

    private MapCameraMode mapCameraMode = MapCameraMode.FREE;
    @Nullable
    private GeoPoint lockedSelectionPoint;
    @Nullable
    private BoundingBox lockedRouteBounds;
    private static final float SELECTION_CAMERA_ZOOM = 17.8f;
    private static final float FOLLOW_CAMERA_ZOOM = 18.2f;
    private final Set<Long> activeRouteWaypointIds = new HashSet<>();
    private final List<AmapRouteStep> lastAmapRouteSteps = new ArrayList<>();

    private static final class AmapRouteStep {
        final String instruction;
        @Nullable
        final GeoPoint point;
        @Nullable
        final String roadName;

        AmapRouteStep(@NonNull String instruction, @Nullable GeoPoint point, @Nullable String roadName) {
            this.instruction = instruction;
            this.point = point;
            this.roadName = roadName;
        }
    }

    private static final class RoutePanelSections {
        final List<RouteWaypointAdapter.Item> directions;
        final List<RouteWaypointAdapter.Item> namedPlaces;

        RoutePanelSections(@NonNull List<RouteWaypointAdapter.Item> directions,
                           @NonNull List<RouteWaypointAdapter.Item> namedPlaces) {
            this.directions = directions;
            this.namedPlaces = namedPlaces;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkInTask = new Runnable() {
        @Override
        public void run() {
            if (!isUiAlive()) {
                return;
            }
            if (autoGuideEnabled) {
                performCheckIn();
            }
            if (isUiAlive()) {
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureAmapPrivacy();
        setContentView(R.layout.map_layout);

        session = SessionManager.from(this);
        if (!session.isLoggedIn()) {
            gotoLogin();
            return;
        }
        api = ApiClient.service(this);

        spotsIntroLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    long locationId = result.getData().getLongExtra(SpotsIntroActivity.EXTRA_LOCATION_ID, -1L);
                    if (locationId > 0) {
                        openLocationOnMap(locationId);
                    }
                });

        spotDetailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    String action = result.getData().getStringExtra(SpotDetailActivity.EXTRA_ACTION);
                    if (SpotDetailActivity.ACTION_ROUTE.equals(action)) {
                        long locationId = result.getData().getLongExtra(
                                SpotDetailActivity.EXTRA_LOCATION_ID, -1L);
                        LocationDto routeTarget = findLocationById(locationId);
                        if (routeTarget != null) {
                            setRouteDestinationAndPlan(routeTarget);
                        } else {
                            planRouteFromSpinners();
                        }
                    } else if (SpotDetailActivity.ACTION_GUIDE.equals(action)) {
                        if (selectedLocation != null) {
                            toggleGuideForLocation(selectedLocation);
                        }
                    }
                });

        bindViews();
        applyWindowInsets();
        geoFenceManager = new GeoFenceManager(AUTO_PLAY_COOLDOWN_MS);
        audioTriggerService = new AudioTriggerService(this, session, this);
        audioTriggerService.setNarrationProvider(this::buildGuideNarration);
        initMapManagers(savedInstanceState);
        initCampusRouteGraph();
        initLocation();
        initNearbyList();
        initRouteWaypointList();
        initSearchUi();
        initActions();
        initRoutePickers();
        initBottomPanelBehavior();
        setupAiEntry();
        updateAutoGuideTile();
        updateTrackTile();
        applyRouteMode(routeMode, false);
        resetLaunchRouteUi();

        verifySession();
        loadLocations();
    }

    /** 初次进入：清空终点与路线，展示校园全景。 */
    private void resetLaunchRouteUi() {
        selectedRouteEndId = ROUTE_END_DESTINATION;
        selectedRouteStartId = ROUTE_START_CURRENT;
        selectedLocation = null;
        routedLocationId = -1L;
        pendingRouteLocationId = -1L;
        pendingRouteMode = null;
        routeDrawGeneration++;
        lastRoutePolylinePoints.clear();
        lastRouteStartPoint = null;
        if (cardRoutePanel != null) {
            cardRoutePanel.setVisibility(View.GONE);
        }
        if (tvRouteSummary != null) {
            tvRouteSummary.setText("");
        }
        if (tvRouteSteps != null) {
            tvRouteSteps.setText("");
        }
        if (routeDirectionsAdapter != null) {
            routeDirectionsAdapter.submit(new ArrayList<>());
        }
        if (routeNamedPlacesAdapter != null) {
            routeNamedPlacesAdapter.submit(new ArrayList<>());
        }
        mapCameraMode = MapCameraMode.FREE;
        lockedRouteBounds = null;
        lockedSelectionPoint = null;
    }

    private void configureAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        try {
            ServiceSettings.updatePrivacyShow(this, true, true);
            ServiceSettings.updatePrivacyAgree(this, true);
            ServiceSettings.getInstance().setApiKey(getString(R.string.amap_api_key));
        } catch (Throwable ignored) {
            // Search SDK may be unavailable on very old builds; map SDK still works.
        }
    }

    private void bindViews() {
        mapView = findViewById(R.id.mapView);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        searchHistoryPanel = findViewById(R.id.searchHistoryPanel);
        rvSearchHistory = findViewById(R.id.rvSearchHistory);
        locateFloatBall = findViewById(R.id.locateFloatBall);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnSettings = findViewById(R.id.btnSettings);
        btnLayer = findViewById(R.id.btnLayer);
        btnFeedback = findViewById(R.id.btnFeedback);
        tileAutoGuide = findViewById(R.id.tileAutoGuide);
        tileTrack = findViewById(R.id.tileTrack);
        tileNavigation = findViewById(R.id.tileNavigation);
        tvLocationName = findViewById(R.id.tvLocationName);
        tvDistance = findViewById(R.id.tvDistance);
        tvDescription = findViewById(R.id.tvDescription);
        tvRouteSummary = findViewById(R.id.tvRouteSummary);
        tvRouteSteps = findViewById(R.id.tvRouteSteps);
        chipRouteWalk = findViewById(R.id.chipRouteWalk);
        chipRouteDrive = findViewById(R.id.chipRouteDrive);
        chipRouteBus = findViewById(R.id.chipRouteBus);
        chipRouteTaxi = findViewById(R.id.chipRouteTaxi);
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavAround = findViewById(R.id.btnNavAround);
        btnNavShare = findViewById(R.id.btnNavShare);
        btnNavMine = findViewById(R.id.btnNavMine);
        btnManualPlay = findViewById(R.id.btnManualPlay);
        btnRoute = findViewById(R.id.btnRoute);
        btnNearestSpots = findViewById(R.id.btnNearestSpots);
        nearbySection = findViewById(R.id.nearbySection);
        mapProgress = findViewById(R.id.mapProgress);
        bottomPanel = findViewById(R.id.bottomPanel);
        bottomPanelHandle = findViewById(R.id.bottomPanelHandle);
        bottomNavBar = findViewById(R.id.bottomNavBar);
        layoutRouteDetail = findViewById(R.id.layoutRouteDetail);
        cardRoutePanel = findViewById(R.id.cardRoutePanel);
        rvRouteDirections = findViewById(R.id.rvRouteDirections);
        rvRouteNamedPlaces = findViewById(R.id.rvRouteNamedPlaces);
        tvGuidePoint = findViewById(R.id.tvGuidePoint);
        spinnerRouteStart = findViewById(R.id.spinnerRouteStart);
        spinnerRouteEnd = findViewById(R.id.spinnerRouteEnd);
        btnSwapRoute = findViewById(R.id.btnSwapRoute);
    }

    private void initRoutePickers() {
        View.OnClickListener openGuidePicker = v -> showLocationPickerDialog();
        if (tvGuidePoint != null) {
            tvGuidePoint.setOnClickListener(openGuidePicker);
        }
        View layoutGuidePoint = findViewById(R.id.layoutGuidePoint);
        if (layoutGuidePoint != null) {
            layoutGuidePoint.setClickable(true);
            layoutGuidePoint.setFocusable(true);
            layoutGuidePoint.setOnClickListener(openGuidePicker);
        }
        refreshRoutePickerOptions();
        if (btnSwapRoute != null) {
            btnSwapRoute.setOnClickListener(v -> swapRouteStartEnd());
        }
        setupSpinnerTouchHelper(spinnerRouteStart);
        setupSpinnerTouchHelper(spinnerRouteEnd);
        if (spinnerRouteStart != null) {
            spinnerRouteStart.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (suppressRouteSpinnerEvents || position < 0 || position >= routeStartOptionIds.size()) {
                        return;
                    }
                    applyRouteStartSelection(routeStartOptionIds.get(position));
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }
        if (spinnerRouteEnd != null) {
            spinnerRouteEnd.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (suppressRouteSpinnerEvents || position < 0 || position >= routeEndOptionIds.size()) {
                        return;
                    }
                    applyRouteEndSelection(routeEndOptionIds.get(position), false);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }
        View layoutRouteStart = findViewById(R.id.layoutRouteStart);
        View layoutRouteEnd = findViewById(R.id.layoutRouteEnd);
        if (layoutRouteStart != null) {
            layoutRouteStart.setOnClickListener(v -> showRouteStartPickerDialog());
        }
        if (layoutRouteEnd != null) {
            layoutRouteEnd.setOnClickListener(v -> showRouteEndPickerDialog());
        }
        refreshRoutePickerOptions();
    }

    private void setupSpinnerTouchHelper(@Nullable Spinner spinner) {
        if (spinner == null) {
            return;
        }
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ViewParent parent = v.getParent();
                while (parent != null) {
                    if (parent instanceof ViewGroup) {
                        ((ViewGroup) parent).requestDisallowInterceptTouchEvent(true);
                    }
                    parent = parent.getParent();
                }
            }
            return false;
        });
    }

    private void refreshRoutePickerOptions() {
        suppressRouteSpinnerEvents = true;
        routeStartOptionIds.clear();
        routeStartOptionLabels.clear();
        routeStartOptionIds.add(ROUTE_START_CURRENT);
        routeStartOptionLabels.add(getString(R.string.route_start_current));
        List<LocationDto> routeSpots = collectRouteSpotOptions();
        for (LocationDto location : routeSpots) {
            routeStartOptionIds.add(routeEndLocId(location.id));
            routeStartOptionLabels.add(location.name);
        }
        if (!routeStartOptionIds.contains(selectedRouteStartId)) {
            selectedRouteStartId = ROUTE_START_CURRENT;
        }

        routeEndOptionIds.clear();
        routeEndOptionLabels.clear();
        routeEndOptionIds.add(ROUTE_END_DESTINATION);
        routeEndOptionLabels.add(getString(R.string.route_end_placeholder));
        for (LocationDto location : routeSpots) {
            routeEndOptionIds.add(routeEndLocId(location.id));
            routeEndOptionLabels.add(location.name);
        }
        if (selectedRouteEndId == null || !routeEndOptionIds.contains(selectedRouteEndId)) {
            selectedRouteEndId = ROUTE_END_DESTINATION;
        }

        if (spinnerRouteStart != null) {
            ArrayAdapter<String> startAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_dropdown_item, routeStartOptionLabels);
            spinnerRouteStart.setAdapter(startAdapter);
            int startIndex = routeStartOptionIds.indexOf(selectedRouteStartId);
            if (startIndex >= 0) {
                spinnerRouteStart.setSelection(startIndex, false);
            }
        }

        if (spinnerRouteEnd != null) {
            ArrayAdapter<String> endAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_dropdown_item, routeEndOptionLabels);
            spinnerRouteEnd.setAdapter(endAdapter);
            int endIndex = routeEndOptionIds.indexOf(selectedRouteEndId);
            if (endIndex >= 0) {
                spinnerRouteEnd.setSelection(endIndex, false);
            }
        }
        suppressRouteSpinnerEvents = false;
        updateGuidePointLabel();
    }

    private void updateGuidePointLabel() {
        if (tvGuidePoint == null) {
            return;
        }
        if (selectedLocation == null) {
            tvGuidePoint.setText(R.string.label_not_selected);
            return;
        }
        tvGuidePoint.setText(selectedLocation.name);
    }

    private void showLocationPickerDialog() {
        showBackendSpotPicker(R.string.dialog_pick_location_title, picked -> {
            applyGuidePointSelection(picked);
        });
    }

    /** 仅设置讲解点并聚焦地图，不修改路线终点。 */
    private void applyGuidePointSelection(@NonNull LocationDto picked) {
        selectedLocation = picked;
        updateGuidePointLabel();
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, picked);
        holdUserMapResult();
        selectLocation(picked, distance, true, false, false);
        focusLocation(picked);
    }

    private void showBackendSpotPicker(int titleRes, @NonNull java.util.function.Consumer<LocationDto> onPick) {
        if (!ensureBackendLocationsForPicker()) {
            return;
        }
        List<LocationDto> options = collectBackendSpotOptions();
        if (options.isEmpty()) {
            toast(getString(R.string.toast_backend_locations_empty));
            return;
        }
        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            LocationDto item = options.get(i);
            labels[i] = formatSpotPickerLabel(item);
        }
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setItems(labels, (dialog, which) -> {
                    if (which >= 0 && which < options.size()) {
                        onPick.accept(options.get(which));
                    }
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    private boolean ensureBackendLocationsForPicker() {
        if (backendLocationsLoaded && !collectBackendSpotOptions().isEmpty()) {
            return true;
        }
        if (!backendLocationsLoaded) {
            toast(getString(R.string.toast_backend_locations_loading));
            loadLocations();
        } else {
            toast(getString(R.string.toast_backend_locations_empty));
        }
        return false;
    }

    @NonNull
    private List<LocationDto> collectBackendSpotOptions() {
        if (!backendLocationsLoaded) {
            return Collections.emptyList();
        }
        List<LocationDto> options = new ArrayList<>();
        for (LocationDto location : locations) {
            if (location != null && hasPlottableLocation(location)) {
                options.add(location);
            }
        }
        options.sort(Comparator.comparing(a -> a.name, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    @NonNull
    private List<LocationDto> collectRouteSpotOptions() {
        List<LocationDto> options = new ArrayList<>();
        for (LocationDto location : locations) {
            if (location != null && hasPlottableLocation(location)) {
                options.add(location);
            }
        }
        options.sort(Comparator.comparing(a -> a.name, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    @NonNull
    private String formatSpotPickerLabel(@NonNull LocationDto item) {
        return item.name + (TextUtils.isEmpty(item.category) ? "" : " · " + item.category);
    }

    private void applyRouteStartSelection(@NonNull String startId) {
        selectedRouteStartId = startId;
        int index = routeStartOptionIds.indexOf(startId);
        if (spinnerRouteStart != null && index >= 0) {
            suppressRouteSpinnerEvents = true;
            spinnerRouteStart.setSelection(index, false);
            suppressRouteSpinnerEvents = false;
        }
        long spotId = parseRouteEndLocId(startId);
        if (spotId > 0) {
            LocationDto startSpot = findLocationById(spotId);
            if (startSpot != null) {
                poiRouteNodeHints.put(startSpot.id, findNearestRouteNode(resolveDisplayPoint(startSpot)));
            }
        }
        if (resolveRouteDestination() != null) {
            showRouteToSelected();
        }
    }

    private void applyRouteEndSelection(@NonNull String endId, boolean fromGuidePicker) {
        selectedRouteEndId = endId;
        int index = routeEndOptionIds.indexOf(endId);
        if (spinnerRouteEnd != null && index >= 0) {
            suppressRouteSpinnerEvents = true;
            spinnerRouteEnd.setSelection(index, false);
            suppressRouteSpinnerEvents = false;
        }
        if (ROUTE_END_DESTINATION.equals(endId)) {
            routedLocationId = -1L;
            pendingRouteLocationId = -1L;
            pendingRouteMode = null;
            clearRoutePolyline();
            if (cardRoutePanel != null) {
                cardRoutePanel.setVisibility(View.GONE);
            }
            if (tvRouteSummary != null) {
                tvRouteSummary.setText("");
            }
            if (tvRouteSteps != null) {
                tvRouteSteps.setText("");
            }
            updateGuidePointLabel();
            return;
        }
        LocationDto destination = findLocationByEndId(endId);
        if (destination == null) {
            return;
        }
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, destination);
        holdUserMapResult();
        clearRouteIfDestinationChanged(destination.id);
        if (distance != null && com.example.gps.util.DistanceFormat.isUsable(distance)) {
            updateRoutePreviewText(distance);
        }
        if (destination.id > 0) {
            poiRouteNodeHints.put(destination.id, findNearestRouteNode(resolveDisplayPoint(destination)));
        }
        showRouteToSelected();
    }

    @Nullable
    private LocationDto findLocationByEndId(@NonNull String endId) {
        long locationId = parseRouteEndLocId(endId);
        if (locationId > 0) {
            return findLocationById(locationId);
        }
        return null;
    }

    private void syncRouteEndWithGuidePoint(@NonNull LocationDto destination) {
        // 讲解点与路线终点已分离，不再自动同步
    }

    private void swapRouteStartEnd() {
        if (TextUtils.isEmpty(selectedRouteStartId) || TextUtils.isEmpty(selectedRouteEndId)) {
            return;
        }
        if (selectedRouteEndId.startsWith(ROUTE_END_LOC_PREFIX)) {
            toast("终点为校园景点，请使用「讲解点」或「终点」列表修改；起点可选路网节点");
            return;
        }
        String temp = selectedRouteStartId;
        selectedRouteStartId = selectedRouteEndId;
        selectedRouteEndId = temp;
        if (ROUTE_END_DESTINATION.equals(selectedRouteStartId)) {
            selectedRouteStartId = ROUTE_START_CURRENT;
        }
        if (ROUTE_START_CURRENT.equals(selectedRouteEndId)) {
            selectedRouteEndId = ROUTE_END_DESTINATION;
        }
        int startIndex = routeStartOptionIds.indexOf(selectedRouteStartId);
        int endIndex = routeEndOptionIds.indexOf(selectedRouteEndId);
        if (spinnerRouteStart != null && startIndex >= 0) {
            spinnerRouteStart.setSelection(startIndex, false);
        }
        if (spinnerRouteEnd != null && endIndex >= 0) {
            spinnerRouteEnd.setSelection(endIndex, false);
        }
        toast("已交换起点与终点");
    }

    private void showRouteStartPickerDialog() {
        if (!ensureBackendLocationsForPicker()) {
            return;
        }
        List<String> labels = new ArrayList<>(routeStartOptionLabels);
        List<String> ids = new ArrayList<>(routeStartOptionIds);
        showRouteNodePickerDialog(
                R.string.dialog_pick_route_start_title,
                labels,
                ids,
                selectedRouteStartId,
                this::applyRouteStartSelection);
    }

    private void showRouteEndPickerDialog() {
        showBackendSpotPicker(R.string.dialog_pick_route_end_title,
                picked -> applyRouteEndSelection(routeEndLocId(picked.id), false));
    }

    private void showRouteNodePickerDialog(int titleRes,
                                           @NonNull List<String> labels,
                                           @NonNull List<String> ids,
                                           @Nullable String currentId,
                                           @NonNull RouteNodePickListener listener) {
        if (labels.isEmpty()) {
            toast("路线节点未初始化");
            return;
        }
        String[] items = labels.toArray(new String[0]);
        int checked = currentId == null ? -1 : ids.indexOf(currentId);
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    if (which >= 0 && which < ids.size()) {
                        listener.onPicked(ids.get(which));
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    private interface RouteNodePickListener {
        void onPicked(@NonNull String nodeId);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.rootMapContainer);
        View topOverlayContainer = findViewById(R.id.topOverlayContainer);
        if (root == null || topOverlayContainer == null || bottomPanel == null || bottomNavBar == null) {
            return;
        }

        final int topPaddingStart = topOverlayContainer.getPaddingStart();
        final int topPaddingTop = topOverlayContainer.getPaddingTop();
        final int topPaddingEnd = topOverlayContainer.getPaddingEnd();
        final int topPaddingBottom = topOverlayContainer.getPaddingBottom();

        final ViewGroup.MarginLayoutParams bottomPanelLpBase =
                (ViewGroup.MarginLayoutParams) bottomPanel.getLayoutParams();
        final int bottomPanelBottomMarginBase = bottomPanelLpBase.bottomMargin;

        final ViewGroup.MarginLayoutParams bottomNavLpBase =
                (ViewGroup.MarginLayoutParams) bottomNavBar.getLayoutParams();
        final int bottomNavBottomMarginBase = bottomNavLpBase.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            topOverlayContainer.setPaddingRelative(
                    topPaddingStart,
                    topPaddingTop + systemBars.top,
                    topPaddingEnd,
                    topPaddingBottom
            );

            ViewGroup.MarginLayoutParams panelLp =
                    (ViewGroup.MarginLayoutParams) bottomPanel.getLayoutParams();
            panelLp.bottomMargin = bottomPanelBottomMarginBase + systemBars.bottom;
            bottomPanel.setLayoutParams(panelLp);

            ViewGroup.MarginLayoutParams navLp =
                    (ViewGroup.MarginLayoutParams) bottomNavBar.getLayoutParams();
            navLp.bottomMargin = bottomNavBottomMarginBase + systemBars.bottom;
            bottomNavBar.setLayoutParams(navLp);

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void initMapManagers(@Nullable Bundle savedInstanceState) {
        campusMapManager = new CampusMapManager(mapView);
        campusMapManager.initialize(this, savedInstanceState);
        defaultMapAnchorApplied = true;
        trackManager = new TrackManager(campusMapManager, this);
        trackManager.setEnabled(trackEnabled);
        if (PermissionUtil.hasLocationPermission(this)) {
            campusMapManager.enableMyLocationBlueDot(true);
        }

        markerManager = new MarkerManager(campusMapManager.getMap(), this);
        markerManager.setGeofenceVisible(geofenceVisible);
        markerManager.setDistanceProvider(location ->
                currentRawGeoPoint == null ? null : distanceRawToLocationMeters(currentRawGeoPoint, location));
        markerManager.setMarkerClickListener((location, distance) -> {
            holdUserMapResult();
            focusLocationOnMap(location, distance);
        });
        mapView.post(() -> applyDefaultMapAnchor(true));
        campusMapManager.getMap().setOnMapLoadedListener(() -> {
            if (!isUiAlive()) {
                return;
            }
            if (lastRoutePolylinePoints.size() >= 2 && routedLocationId >= 0 && resolveRouteDestination() != null) {
                redrawStoredRoutePolyline();
            } else if (routePolyline == null && routedLocationId < 0 && mapCameraMode == MapCameraMode.FREE) {
                applyDefaultMapAnchor(false);
            }
        });

        markerManager.setInfoWindowActionListener(new PoiInfoWindow.ActionListener() {
            @Override
            public void onGuide(@NonNull LocationDto location) {
                toggleGuideForLocation(location);
            }

            @Override
            public void onRoute(@NonNull LocationDto location) {
                setRouteDestinationAndPlan(location);
            }

            @Override
            public void onDetail(@NonNull LocationDto location) {
                openLocationDetail(location);
            }
        });
        markerManager.setPoiRenderedCallback((location, displayPoint) ->
                poiRouteNodeHints.put(location.id, findNearestRouteNode(displayPoint)));
        initAmapPoiClickListener();
        initRouteSearch();
    }

    private void initAmapPoiClickListener() {
        if (campusMapManager == null || campusMapManager.getMap() == null) {
            return;
        }
        campusMapManager.getMap().setTouchPoiEnable(true);
        campusMapManager.getMap().setOnPOIClickListener(this::showAmapMapPoiDetail);
    }

    private void initRouteSearch() {
        try {
            routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
                @Override
                public void onBusRouteSearched(BusRouteResult result, int code) {
                    handleAmapBusRouteResult(result, code);
                }

                @Override
                public void onDriveRouteSearched(DriveRouteResult result, int code) {
                    handleAmapDriveRouteResult(result, code);
                }

                @Override
                public void onWalkRouteSearched(WalkRouteResult result, int code) {
                    handleAmapWalkRouteResult(result, code);
                }

                @Override
                public void onRideRouteSearched(RideRouteResult result, int code) {
                    // The current UI does not expose riding navigation.
                }
            });
        } catch (AMapException e) {
            routeSearch = null;
        }
    }

    private void initLocation() {
        locationService = new LocationService(this);
        locationService.setCallback(new LocationService.Callback() {
            @Override
            public void onLocationUpdated(@NonNull AMapLocation location) {
                updateCurrentLocation(location);
            }

            @Override
            public void onLocationFailed(int errorCode, @NonNull String errorInfo) {
                updateStatus("地图实时状态：定位失败 " + errorCode + " - " + errorInfo);
            }
        });
        if (!locationService.initialize()) {
            updateStatus("地图实时状态：高德定位初始化失败");
        }
    }

    private void initRouteWaypointList() {
        int spotColor = getColor(R.color.route_spot_highlight);
        int directionColor = getColor(R.color.text_main);
        int namedPlaceColor = getColor(R.color.text_main);
        RouteWaypointAdapter.Listener spotClickListener = item -> {
            LocationDto loc = findLocationById(item.locationId);
            if (loc != null) {
                Double distance = currentRawGeoPoint == null
                        ? null
                        : distanceRawToLocationMeters(currentRawGeoPoint, loc);
                List<String> nearbyNames = new ArrayList<>();
                for (LocationDto nearby : findNearestPoisOf(loc, 3)) {
                    if (nearby != null && !TextUtils.isEmpty(nearby.name)) {
                        nearbyNames.add(nearby.name);
                    }
                }
                spotDetailLauncher.launch(SpotDetailActivity.buildIntent(this, loc, distance, nearbyNames));
            }
        };
        if (rvRouteDirections != null) {
            rvRouteDirections.setLayoutManager(new LinearLayoutManager(this));
            routeDirectionsAdapter = new RouteWaypointAdapter(spotClickListener, spotColor, directionColor);
            rvRouteDirections.setAdapter(routeDirectionsAdapter);
        }
        if (rvRouteNamedPlaces != null) {
            rvRouteNamedPlaces.setLayoutManager(new LinearLayoutManager(this));
            routeNamedPlacesAdapter = new RouteWaypointAdapter(spotClickListener, spotColor, namedPlaceColor);
            rvRouteNamedPlaces.setAdapter(routeNamedPlacesAdapter);
        }
    }

    /** 点击围栏/Marker：仅聚焦地图，不设为路线终点。 */
    private void focusLocationOnMap(@NonNull LocationDto location, @Nullable Double distance) {
        focusCampusSpotOnMap(location, distance);
    }

    /** 附近景点或数据库景点：聚焦并弹出讲解/路线/详情气泡。 */
    private void focusCampusSpotOnMap(@NonNull LocationDto location, @Nullable Double distance) {
        resetManualGuideIfLocationChanged(location.id);
        holdUserMapResult();
        selectLocation(location, distance, true, false, false);
        focusLocation(location);
    }

    /** 气泡或底栏「路线」：仅设为终点并规划，不改变讲解点。 */
    private void setRouteDestinationAndPlan(@NonNull LocationDto location) {
        holdUserMapResult();
        selectRouteEndCandidate(location);
        clearRouteIfDestinationChanged(location.id);
        if (location.id > 0) {
            poiRouteNodeHints.put(location.id, findNearestRouteNode(resolveDisplayPoint(location)));
        }
        showRouteToSelected();
    }

    private void selectRouteEndCandidate(@NonNull LocationDto location) {
        if (hasPlottableLocation(location)) {
            upsertSearchLocation(location);
        }
        String endId = routeEndLocId(location.id);
        if (!routeEndOptionIds.contains(endId)) {
            refreshRoutePickerOptions();
        }
        selectedRouteEndId = endId;
        int index = routeEndOptionIds.indexOf(endId);
        if (spinnerRouteEnd != null && index >= 0) {
            suppressRouteSpinnerEvents = true;
            spinnerRouteEnd.setSelection(index, false);
            suppressRouteSpinnerEvents = false;
        }
    }

    private void initNearbyList() {
        RecyclerView rvNearby = findViewById(R.id.rvNearby);
        rvNearby.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        nearbyAdapter = new NearbyLocationAdapter((location, distance) -> {
            focusLocationOnMap(location, distance);
        });
        rvNearby.setAdapter(nearbyAdapter);
    }

    private void initSearchUi() {
        searchHistoryStore = new SearchHistoryStore(this);
        rvSearchHistory.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        searchHistoryAdapter = new SearchHistoryAdapter(keyword -> {
            etSearch.setText(keyword);
            etSearch.setSelection(keyword.length());
            performSearch();
        });
        rvSearchHistory.setAdapter(searchHistoryAdapter);

        etSearch.setOnFocusChangeListener((v, hasFocus) -> updateSearchOverlayVisibility());
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSearchOverlayVisibility();
            }
        });
        updateSearchOverlayVisibility();
    }

    private void updateSearchOverlayVisibility() {
        String keyword = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        // 仅当用户点击搜索框并输入内容时才展示
        boolean searching = etSearch.hasFocus() && !keyword.isEmpty();

        if (!searching) {
            btnSearch.setVisibility(View.GONE);
            searchHistoryPanel.setVisibility(View.GONE);
            return;
        }

        btnSearch.setVisibility(View.VISIBLE);
        setBottomPanelCollapsed(true, true);
        View topOverlay = findViewById(R.id.topOverlayContainer);
        if (topOverlay != null) {
            topOverlay.bringToFront();
            ensureFloatOverlaysOnTop();
        }

        List<String> history = searchHistoryStore.filterByPrefix(keyword);
        searchHistoryAdapter.submit(history);
        searchHistoryPanel.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void initActions() {
        btnZoomIn.setOnClickListener(v -> zoomInMap());
        btnZoomOut.setOnClickListener(v -> zoomOutMap());

        btnManualPlay.setOnClickListener(v -> {
            if (selectedLocation != null) {
                toggleGuideForLocation(selectedLocation);
            } else {
                toast(getString(R.string.toast_select_location_first));
            }
        });
        btnRoute.setOnClickListener(v -> planRouteFromSpinners());
        btnNearestSpots.setOnClickListener(v -> showNearestSpotsPanel());
        chipRouteWalk.setOnClickListener(v -> applyRouteMode(RouteMode.WALK, true));
        chipRouteDrive.setOnClickListener(v -> applyRouteMode(RouteMode.DRIVE, true));
        chipRouteBus.setOnClickListener(v -> applyRouteMode(RouteMode.BUS, true));
        chipRouteTaxi.setOnClickListener(v -> applyRouteMode(RouteMode.TAXI, true));

        TextView serviceHotel = findViewById(R.id.serviceHotel);
        TextView serviceFood = findViewById(R.id.serviceFood);
        TextView serviceSpot = findViewById(R.id.serviceSpot);
        serviceHotel.setOnClickListener(v -> openShareHub("HOTEL"));
        serviceFood.setOnClickListener(v -> openShareHub("FOOD"));
        serviceSpot.setOnClickListener(v ->
                spotsIntroLauncher.launch(new Intent(this, SpotsIntroActivity.class)));
        tileNavigation.setOnClickListener(v -> planRouteFromSpinners());
        tileAutoGuide.setOnClickListener(v -> toggleAutoGuide());
        tileTrack.setOnClickListener(v -> toggleTrack());

        btnLayer.setOnClickListener(v -> switchTileLayer());
        btnLayer.setOnLongClickListener(v -> {
            geofenceVisible = !geofenceVisible;
            markerManager.setGeofenceVisible(geofenceVisible);
            redrawGeofences();
            toast(getString(geofenceVisible ? R.string.toast_geofence_on : R.string.toast_geofence_off));
            return true;
        });

        btnFeedback.setOnClickListener(v -> FeedbackDialogHelper.show(this));
        btnSettings.setOnClickListener(this::showSettingsMenu);

        btnNavHome.setOnClickListener(v -> {
            setActiveNavTab(0);
            if (!reapplyMapCameraLock()) {
                focusCampusOverview();
            }
        });
        btnNavAround.setOnClickListener(v ->
                spotsIntroLauncher.launch(new Intent(this, SpotsIntroActivity.class)));
        btnNavShare.setOnClickListener(v -> {
            setActiveNavTab(2);
            openShareHub(null);
        });
        btnNavMine.setOnClickListener(v -> {
            setActiveNavTab(3);
            startActivity(new Intent(this, ProfileActivity.class));
        });
        applyRouteMode(RouteMode.WALK, false);
        setActiveNavTab(0);

        btnSearch.setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private static final String PREFS_AI_FLOAT = "map_ai_float";
    private static final String PREFS_LOCATE_FLOAT = "map_locate_float";
    private static final String KEY_FLOAT_X = "x";
    private static final String KEY_FLOAT_Y = "y";

    private void ensureFloatOverlaysOnTop() {
        View overlay = findViewById(R.id.aiFloatOverlay);
        View root = findViewById(R.id.rootMapContainer);
        if (overlay != null && root instanceof ViewGroup) {
            ((ViewGroup) root).bringChildToFront(overlay);
            overlay.setTranslationZ(100f);
        }
        if (locateFloatBall != null) {
            locateFloatBall.setTranslationZ(101f);
        }
        View aiBall = findViewById(R.id.aiFloatBall);
        if (aiBall != null) {
            aiBall.setTranslationZ(102f);
        }
    }

    private void setupAiEntry() {
        setupLocateFloatBall();
        View ball = findViewById(R.id.aiFloatBall);
        View overlay = findViewById(R.id.aiFloatOverlay);
        View root = findViewById(R.id.rootMapContainer);
        View dragBounds = overlay != null ? overlay : root;
        ensureFloatOverlaysOnTop();

        View.OnClickListener openAi = v -> startActivity(new Intent(this, AiChatActivity.class));

        ball.post(() -> {
            android.content.SharedPreferences sp = getSharedPreferences(PREFS_AI_FLOAT, MODE_PRIVATE);
            if (sp.contains(KEY_FLOAT_X)) {
                ball.setX(sp.getFloat(KEY_FLOAT_X, 0f));
                ball.setY(sp.getFloat(KEY_FLOAT_Y, 0f));
            } else {
                placeAiFloatBallDefault(ball);
            }
            clampFloatBall(ball, dragBounds);
        });

        attachDraggableFloatBall(
                ball,
                dragBounds,
                PREFS_AI_FLOAT,
                openAi,
                null);
    }

    private void setupLocateFloatBall() {
        if (locateFloatBall == null) {
            return;
        }
        View overlay = findViewById(R.id.aiFloatOverlay);
        View root = findViewById(R.id.rootMapContainer);
        View dragBounds = overlay != null ? overlay : root;
        locateFloatBall.post(() -> {
            android.content.SharedPreferences sp = getSharedPreferences(PREFS_LOCATE_FLOAT, MODE_PRIVATE);
            if (sp.contains(KEY_FLOAT_X)) {
                locateFloatBall.setX(sp.getFloat(KEY_FLOAT_X, 0f));
                locateFloatBall.setY(sp.getFloat(KEY_FLOAT_Y, 0f));
            } else {
                placeLocateFloatBallDefault(locateFloatBall, dragBounds);
            }
            clampFloatBall(locateFloatBall, dragBounds);
        });
        attachDraggableFloatBall(
                locateFloatBall,
                dragBounds,
                PREFS_LOCATE_FLOAT,
                v -> centerToCurrent(),
                () -> {
                    followMyLocation = !followMyLocation;
                    toast(followMyLocation
                            ? getString(R.string.toast_follow_location_on)
                            : getString(R.string.toast_follow_location_off));
                });
    }

    private void attachDraggableFloatBall(@NonNull View ball,
                                          @NonNull View dragBounds,
                                          @NonNull String prefsName,
                                          @NonNull View.OnClickListener onClick,
                                          @Nullable Runnable onLongPressToggle) {
        float slop = 8f * getResources().getDisplayMetrics().density;
        final float[] lastRaw = new float[2];
        final boolean[] dragged = {false};
        final long[] downTime = {0L};
        ball.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastRaw[0] = event.getRawX();
                    lastRaw[1] = event.getRawY();
                    dragged[0] = false;
                    downTime[0] = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastRaw[0];
                    float dy = event.getRawY() - lastRaw[1];
                    if (!dragged[0] && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                        dragged[0] = true;
                    }
                    if (dragged[0]) {
                        v.setX(v.getX() + dx);
                        v.setY(v.getY() + dy);
                        clampFloatBall(v, dragBounds);
                        lastRaw[0] = event.getRawX();
                        lastRaw[1] = event.getRawY();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!dragged[0]) {
                        if (onLongPressToggle != null
                                && System.currentTimeMillis() - downTime[0] > 500L) {
                            onLongPressToggle.run();
                        } else {
                            onClick.onClick(v);
                        }
                    } else {
                        getSharedPreferences(prefsName, MODE_PRIVATE).edit()
                                .putFloat(KEY_FLOAT_X, v.getX())
                                .putFloat(KEY_FLOAT_Y, v.getY())
                                .apply();
                    }
                    ensureFloatOverlaysOnTop();
                    return true;
                default:
                    return false;
            }
        });
    }

    private void placeAiFloatBallDefault(View ball) {
        View overlay = findViewById(R.id.aiFloatOverlay);
        View top = findViewById(R.id.topOverlayContainer);
        View search = findViewById(R.id.searchContainer);
        float margin = 12f * getResources().getDisplayMetrics().density;
        float gap = 8f * getResources().getDisplayMetrics().density;
        float baseX = top.getLeft() + margin;
        float baseY = top.getTop() + search.getTop() + search.getHeight() + gap;
        if (overlay != null) {
            int[] topLoc = new int[2];
            int[] overlayLoc = new int[2];
            top.getLocationOnScreen(topLoc);
            overlay.getLocationOnScreen(overlayLoc);
            baseX = topLoc[0] - overlayLoc[0] + margin;
            baseY = topLoc[1] - overlayLoc[1] + search.getTop() + search.getHeight() + gap;
        }
        ball.setX(baseX);
        ball.setY(baseY);
    }

    private void placeLocateFloatBallDefault(@NonNull View ball, @NonNull View bounds) {
        float margin = 14f * getResources().getDisplayMetrics().density;
        float x = Math.max(margin, bounds.getWidth() - ball.getWidth() - margin);
        float y = Math.max(margin, bounds.getHeight() * 0.58f);
        ball.setX(x);
        ball.setY(y);
    }

    private void clampFloatBall(View ball, View bounds) {
        float maxX = Math.max(0, bounds.getWidth() - ball.getWidth());
        float maxY = Math.max(0, bounds.getHeight() - ball.getHeight());
        ball.setX(Math.max(0f, Math.min(ball.getX(), maxX)));
        ball.setY(Math.max(0f, Math.min(ball.getY(), maxY)));
    }

    private void performSearch() {
        String keyword = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            toast(getString(R.string.toast_input_place));
            return;
        }
        searchHistoryStore.addKeyword(keyword);
        updateSearchOverlayVisibility();
        hideKeyboard(etSearch);
        etSearch.clearFocus();
        searchLocationByName(keyword);
    }

    private void hideKeyboard(@Nullable View view) {
        if (view == null) {
            return;
        }
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void holdUserMapResult() {
        keepMapOnUserResult = true;
        followMyLocation = false;
    }

    private void unlockMapCameraForFollow() {
        mapCameraMode = MapCameraMode.FREE;
        lockedSelectionPoint = null;
        lockedRouteBounds = null;
    }

    private void lockMapCameraOnSelection(@NonNull GeoPoint point) {
        mapCameraMode = MapCameraMode.LOCKED_ON_SELECTION;
        lockedSelectionPoint = point;
        lockedRouteBounds = null;
        followMyLocation = false;
        keepMapOnUserResult = true;
        hasAutoCentered = true;
        if (isMapReady()) {
            campusMapManager.setCenterAndZoom(point, SELECTION_CAMERA_ZOOM);
        }
    }

    private void lockMapCameraOnRoute(@NonNull BoundingBox box) {
        mapCameraMode = MapCameraMode.LOCKED_ON_ROUTE;
        lockedRouteBounds = box;
        lockedSelectionPoint = null;
        followMyLocation = false;
        keepMapOnUserResult = true;
        hasAutoCentered = true;
        if (isMapReady()) {
            campusMapManager.zoomToBoundingBox(box, true);
            campusMapManager.invalidate();
        }
    }

    private void focusMapOnRoute(@NonNull List<GeoPoint> routePoints) {
        if (!isMapReady() || routePoints.size() < 2) {
            return;
        }
        BoundingBox box = buildBoundingBoxFromRoutePoints(routePoints);
        lockMapCameraOnRoute(box);
        campusMapManager.zoomToRoutePoints(routePoints, true);
    }

    /** 按已解析好的路径点同步绘制折线（对齐 swu_gps，避免异步绘制被 generation 取消）。 */
    private void drawRoutePathPointsResolved(@NonNull List<GeoPoint> pathPoints) {
        if (pathPoints.size() < 2 || !isMapReady()) {
            return;
        }
        drawRoutePolylineNow(pathPoints);
        focusMapOnRoute(pathPoints);
    }

    /** 按路径点绘制导航折线并缩放至路线范围（不再二次改写高德/兜底折线）。 */
    private void drawRoutePathPoints(@NonNull List<GeoPoint> pathPoints) {
        drawRoutePathPointsResolved(dedupeExactRoutePoints(pathPoints));
    }

    private void redrawStoredRoutePolyline() {
        if (lastRoutePolylinePoints.size() < 2) {
            return;
        }
        drawRoutePolylineNow(lastRoutePolylinePoints);
        focusMapOnRoute(lastRoutePolylinePoints);
    }

    /**
     * 地图绘制专用：直接使用高德/兜底返回的折线，仅衔接到起终点，供文字面板以外的轨迹展示。
     */
    @NonNull
    private List<GeoPoint> prepareMapRouteDrawPoints(@NonNull List<GeoPoint> routePoints,
                                                     @Nullable GeoPoint routeStart,
                                                     @Nullable GeoPoint routeEnd) {
        List<GeoPoint> merged = dedupeExactRoutePoints(routePoints);
        if (merged.size() >= 2 && routeStart != null && routeEnd != null) {
            return stitchRouteEndpoints(merged, routeStart, routeEnd);
        }
        if (merged.size() >= 2) {
            return merged;
        }
        if (routeStart != null && routeEnd != null) {
            List<GeoPoint> fallback = new ArrayList<>();
            fallback.add(routeStart);
            fallback.add(routeEnd);
            return fallback;
        }
        return merged;
    }

    /**
     * 地图绘制：优先高德沿路折线；若折线过短（常见于校内短距规划），改用与文字面板一致的参考路径。
     */
    @NonNull
    private List<GeoPoint> chooseMapRouteDrawPoints(@NonNull List<GeoPoint> amapPoints,
                                                    @Nullable LocationDto destination,
                                                    double amapReportedDistanceMeters) {
        GeoPoint routeStart = lastRouteStartPoint;
        GeoPoint routeEnd = destination == null ? null : resolveRouteEndPoint(destination);
        List<GeoPoint> amapDraw = prepareMapRouteDrawPoints(amapPoints, routeStart, routeEnd);
        if (destination == null || routeStart == null || routeEnd == null) {
            return amapDraw;
        }

        List<GeoPoint> campusDraw = prepareMapRouteDrawPoints(
                buildCampusRoute(routeStart, routeEnd, destination), routeStart, routeEnd);
        double amapSpan = calculatePathDistanceMeters(amapDraw);
        double campusSpan = calculatePathDistanceMeters(campusDraw);
        double directDistance = calculateDistanceMeters(
                routeStart.getLatitude(), routeStart.getLongitude(),
                routeEnd.getLatitude(), routeEnd.getLongitude());
        double minUsableSpan = Math.max(60.0, directDistance * 0.18);

        boolean amapLooksComplete = amapDraw.size() >= 3 && amapSpan >= minUsableSpan;
        if (!amapLooksComplete && amapReportedDistanceMeters > 0) {
            amapLooksComplete = amapSpan >= amapReportedDistanceMeters * 0.45;
        }
        if (campusDraw.size() >= 2 && !amapLooksComplete) {
            return campusDraw;
        }
        return amapDraw;
    }

    /** 文字途径点面板：仍可使用校内参考路径，与地图折线绘制分离。 */
    @NonNull
    private List<GeoPoint> buildRoutePanelReference(@NonNull List<GeoPoint> routePoints,
                                                    @NonNull LocationDto destination) {
        GeoPoint routeStart = lastRouteStartPoint;
        GeoPoint routeEnd = resolveRouteEndPoint(destination);
        if (routeStart == null || routeEnd == null) {
            return routePoints;
        }
        List<GeoPoint> campusReference = buildCampusRoute(routeStart, routeEnd, destination);
        if (calculatePathDistanceMeters(campusReference)
                > calculatePathDistanceMeters(routePoints) * 1.1) {
            return campusReference;
        }
        return routePoints;
    }

    @NonNull
    private List<GeoPoint> stitchRouteEndpoints(@NonNull List<GeoPoint> path,
                                                @NonNull GeoPoint routeStart,
                                                @NonNull GeoPoint routeEnd) {
        List<GeoPoint> stitched = new ArrayList<>();
        if (path.isEmpty()) {
            stitched.add(routeStart);
            stitched.add(routeEnd);
            return stitched;
        }
        GeoPoint first = path.get(0);
        if (calculateDistanceMeters(first.getLatitude(), first.getLongitude(),
                routeStart.getLatitude(), routeStart.getLongitude()) > 20.0) {
            appendRoutePolylinePoint(stitched, routeStart);
        }
        for (GeoPoint point : path) {
            appendRoutePolylinePoint(stitched, point);
        }
        GeoPoint last = stitched.get(stitched.size() - 1);
        if (calculateDistanceMeters(last.getLatitude(), last.getLongitude(),
                routeEnd.getLatitude(), routeEnd.getLongitude()) > 20.0) {
            appendRoutePolylinePoint(stitched, routeEnd);
        }
        return stitched.size() >= 2 ? stitched : path;
    }

    /** 在地图上按路径点绘制导航折线（对齐 swu_gps 的单折线绘制方式）。 */
    private void drawRoutePolylineNow(@NonNull List<GeoPoint> pathPoints) {
        if (!isMapReady() || pathPoints.size() < 2 || campusMapManager.getMap() == null) {
            return;
        }
        lastRoutePolylinePoints = new ArrayList<>(pathPoints);
        clearRoutePolylineOnly();
        routePolyline = campusMapManager.addPolyline(
                pathPoints, getRouteColor(routeMode), 10f);
        campusMapManager.invalidate();
    }

    private void renderActiveRoutePolyline(@NonNull List<GeoPoint> pathPoints) {
        drawRoutePathPoints(pathPoints);
    }

    @NonNull
    private static BoundingBox buildBoundingBoxFromRoutePoints(@NonNull List<GeoPoint> points) {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        for (GeoPoint point : points) {
            if (point == null) {
                continue;
            }
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }
        if (minLat == Double.MAX_VALUE) {
            return new BoundingBox(
                    CampusMapManager.CAMPUS_CENTER_LAT + 0.01,
                    CampusMapManager.CAMPUS_CENTER_LAT - 0.01,
                    CampusMapManager.CAMPUS_CENTER_LNG + 0.01,
                    CampusMapManager.CAMPUS_CENTER_LNG - 0.01);
        }
        double padLat = Math.max(0.0006, (maxLat - minLat) * 0.15);
        double padLon = Math.max(0.0006, (maxLon - minLon) * 0.15);
        return new BoundingBox(maxLat + padLat, minLat - padLat, maxLon + padLon, minLon - padLon);
    }

    private boolean shouldAutoMoveCamera() {
        return mapCameraMode == MapCameraMode.FREE;
    }

    private boolean shouldMoveSelectionCamera(@NonNull GeoPoint target) {
        if (mapCameraMode != MapCameraMode.LOCKED_ON_SELECTION || lockedSelectionPoint == null) {
            return true;
        }
        return calculateDistanceMeters(
                lockedSelectionPoint.getLatitude(),
                lockedSelectionPoint.getLongitude(),
                target.getLatitude(),
                target.getLongitude()) > 8.0;
    }

    /** 恢复景点/路线聚焦；成功返回 true。 */
    private boolean reapplyMapCameraLock() {
        if (!isMapReady()) {
            return false;
        }
        if (mapCameraMode == MapCameraMode.LOCKED_ON_ROUTE && lockedRouteBounds != null) {
            campusMapManager.zoomToBoundingBox(lockedRouteBounds, false);
            campusMapManager.invalidate();
            return true;
        }
        if (mapCameraMode == MapCameraMode.LOCKED_ON_SELECTION) {
            GeoPoint point = lockedSelectionPoint;
            if (point == null && selectedLocation != null) {
                point = resolveDisplayPoint(selectedLocation);
            }
            if (point != null) {
                lockedSelectionPoint = point;
                campusMapManager.setCenterAndZoom(point, SELECTION_CAMERA_ZOOM);
                return true;
            }
        }
        return false;
    }

    private void releaseUserMapResult() {
        keepMapOnUserResult = false;
        routedLocationId = -1L;
        pendingRouteLocationId = -1L;
        pendingRouteMode = null;
        clearRoutePolyline();
    }

    private void clearRouteIfDestinationChanged(long locationId) {
        if ((routedLocationId >= 0 && routedLocationId != locationId)
                || (pendingRouteLocationId >= 0 && pendingRouteLocationId != locationId)) {
            routedLocationId = -1L;
            pendingRouteLocationId = -1L;
            pendingRouteMode = null;
            clearRoutePolyline();
        }
    }

    private void clearRoutePolylineOnly() {
        routeLandmarkSearchToken++;
        if (routePolylineOutline != null) {
            routePolylineOutline.remove();
            routePolylineOutline = null;
        }
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
    }

    private void clearRoutePolyline() {
        clearRoutePolylineOnly();
        activeRouteWaypointIds.clear();
        lastAmapRouteSteps.clear();
        if (mapCameraMode == MapCameraMode.LOCKED_ON_ROUTE) {
            mapCameraMode = MapCameraMode.FREE;
            lockedRouteBounds = null;
            applyDefaultMapAnchor(true);
        }
        if (cardRoutePanel != null) {
            cardRoutePanel.setVisibility(View.GONE);
        }
        if (routeDirectionsAdapter != null) {
            routeDirectionsAdapter.submit(new ArrayList<>());
        }
        if (routeNamedPlacesAdapter != null) {
            routeNamedPlacesAdapter.submit(new ArrayList<>());
        }
        lastRoutePolylinePoints.clear();
    }

    private void initBottomPanelBehavior() {
        if (bottomPanel == null || bottomPanelHandle == null) {
            return;
        }
        bottomPanel.post(() -> {
            applyBottomPanelHeightLimit();
            panelCollapsedTranslation = Math.max(0f, bottomPanel.getHeight() - dpToPx(BOTTOM_PANEL_PEEK_DP));
            setBottomPanelCollapsed(false, false);
        });
        bottomPanelHandle.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    panelTouchDownY = event.getRawY();
                    panelTouchDownTranslation = bottomPanel.getTranslationY();
                    panelDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - panelTouchDownY;
                    float next = Math.max(0f, Math.min(panelCollapsedTranslation, panelTouchDownTranslation + deltaY));
                    if (Math.abs(deltaY) > dpToPx(4)) {
                        panelDragging = true;
                    }
                    bottomPanel.setTranslationY(next);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!panelDragging) {
                        toggleBottomPanel();
                        return true;
                    }
                    boolean collapse = bottomPanel.getTranslationY() > panelCollapsedTranslation * 0.42f;
                    setBottomPanelCollapsed(collapse, true);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void applyBottomPanelHeightLimit() {
        if (bottomPanel == null) {
            return;
        }
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int maxByScreen = (int) (metrics.heightPixels * BOTTOM_PANEL_MAX_HEIGHT_FRACTION);
        int maxHeight = Math.min(dpToPx(BOTTOM_PANEL_MAX_HEIGHT_DP), maxByScreen);
        ViewGroup.LayoutParams lp = bottomPanel.getLayoutParams();
        if (lp.height > maxHeight) {
            lp.height = maxHeight;
            bottomPanel.setLayoutParams(lp);
        }
    }

    private void toggleBottomPanel() {
        setBottomPanelCollapsed(!bottomPanelCollapsed, true);
        bottomPanel.post(this::ensureFloatOverlaysOnTop);
    }

    private void setBottomPanelCollapsed(boolean collapsed, boolean animate) {
        if (bottomPanel == null) {
            return;
        }
        bottomPanelCollapsed = collapsed;
        float target = collapsed ? panelCollapsedTranslation : 0f;
        if (animate) {
            bottomPanel.animate().translationY(target).setDuration(220)
                    .withEndAction(this::ensureFloatOverlaysOnTop)
                    .start();
        } else {
            bottomPanel.setTranslationY(target);
            ensureFloatOverlaysOnTop();
        }
    }

    private void verifySession() {
        api.me().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<UserDto>> call, Response<ApiResponse<UserDto>> response) {
                ApiResponse<UserDto> body = response.body();
                if (response.code() == 401) {
                    session.clearAuth();
                    toast(getString(R.string.toast_auth_expired));
                    gotoLogin();
                    return;
                }
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    updateStatus("地图实时状态：离线浏览模式");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserDto>> call, Throwable throwable) {
                // Keep current session for temporary network issues.
            }
        });
    }

    private void showSettingsMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.inflate(R.menu.menu_map);
        popupMenu.setOnMenuItemClickListener(this::onMenuItemClick);
        popupMenu.show();
    }

    private boolean onMenuItemClick(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_my_routes) {
            showMyRoutesPicker();
            return true;
        }
        if (id == R.id.action_refresh) {
            loadLocations();
            return true;
        }
        if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return false;
    }

    private void showMyRoutesPicker() {
        if (!session.isLoggedIn()) {
            toast(getString(R.string.toast_my_routes_login));
            return;
        }
        api.getMyRoutes().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UserRouteSummaryDto>>> call,
                                   Response<ApiResponse<List<UserRouteSummaryDto>>> response) {
                ApiResponse<List<UserRouteSummaryDto>> body = response.body();
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    toast(getString(R.string.toast_my_routes_load_failed));
                    return;
                }
                List<UserRouteSummaryDto> routes = body.data;
                if (routes.isEmpty()) {
                    toast(getString(R.string.toast_my_routes_empty));
                    return;
                }
                String[] labels = new String[routes.size()];
                for (int i = 0; i < routes.size(); i++) {
                    UserRouteSummaryDto route = routes.get(i);
                    String distance = route.totalDistanceMeters != null
                            ? String.format(Locale.CHINA, " · %.1fkm", route.totalDistanceMeters / 1000.0)
                            : "";
                    labels[i] = route.name + distance + " · " + route.waypointCount + "站";
                }
                new AlertDialog.Builder(MapActivity.this)
                        .setTitle(R.string.dialog_my_routes_title)
                        .setItems(labels, (dialog, which) -> loadSavedRoute(routes.get(which).id))
                        .setNegativeButton(R.string.btn_close, null)
                        .show();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UserRouteSummaryDto>>> call, Throwable t) {
                toast(getString(R.string.toast_my_routes_load_failed));
            }
        });
    }

    private void loadSavedRoute(long routeId) {
        api.getRouteDetail(routeId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<UserRouteDetailDto>> call,
                                   Response<ApiResponse<UserRouteDetailDto>> response) {
                ApiResponse<UserRouteDetailDto> body = response.body();
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    toast(getString(R.string.toast_my_routes_load_failed));
                    return;
                }
                applySavedRoute(body.data);
            }

            @Override
            public void onFailure(Call<ApiResponse<UserRouteDetailDto>> call, Throwable t) {
                toast(getString(R.string.toast_my_routes_load_failed));
            }
        });
    }

    private void applySavedRoute(@NonNull UserRouteDetailDto detail) {
        if (detail.waypoints == null || detail.waypoints.isEmpty()) {
            toast(getString(R.string.toast_my_routes_empty));
            return;
        }
        activeSavedRouteId = detail.id;
        holdUserMapResult();
        setBottomPanelCollapsed(false, true);

        List<UserRouteDetailDto.WaypointDto> stops = new ArrayList<>(detail.waypoints);
        stops.sort(Comparator.comparingInt(w -> w.waypointOrder));

        List<Long> locationIds = new ArrayList<>();
        for (UserRouteDetailDto.WaypointDto stop : stops) {
            if (stop.locationId != null) {
                locationIds.add(stop.locationId);
            }
        }

        LocationDto firstStop = null;
        for (Long locationId : locationIds) {
            LocationDto loc = findLocationById(locationId);
            if (loc != null) {
                firstStop = loc;
                break;
            }
        }
        if (firstStop != null) {
            Double distance = currentRawGeoPoint == null
                    ? null
                    : distanceRawToLocationMeters(currentRawGeoPoint, firstStop);
            selectLocation(firstStop, distance);
            focusLocation(firstStop);
        }

        if (locationIds.size() >= 2) {
            Double startLat = currentRawGeoPoint != null ? currentRawGeoPoint.getLatitude() : null;
            Double startLng = currentRawGeoPoint != null ? currentRawGeoPoint.getLongitude() : null;
            String mode = routeMode.name().toLowerCase(Locale.ROOT);
            MultiStopRouteReq req = new MultiStopRouteReq(startLat, startLng, locationIds, mode);
            api.planMultiStopRoute(req).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<RoutePlanDto>> call,
                                       Response<ApiResponse<RoutePlanDto>> response) {
                    ApiResponse<RoutePlanDto> body = response.body();
                    if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                        renderSavedRouteFromWaypoints(detail, stops);
                        return;
                    }
                    renderBackendRoutePlan(body.data, detail.name);
                }

                @Override
                public void onFailure(Call<ApiResponse<RoutePlanDto>> call, Throwable t) {
                    renderSavedRouteFromWaypoints(detail, stops);
                }
            });
        } else {
            renderSavedRouteFromWaypoints(detail, stops);
        }

        toast(getString(R.string.toast_my_route_applied, detail.name));
    }

    private void renderSavedRouteFromWaypoints(@NonNull UserRouteDetailDto detail,
                                               @NonNull List<UserRouteDetailDto.WaypointDto> stops) {
        List<GeoPoint> points = new ArrayList<>();
        for (UserRouteDetailDto.WaypointDto stop : stops) {
            points.add(new GeoPoint(stop.latitude, stop.longitude));
        }
        if (currentGeoPoint != null && !points.isEmpty()) {
            GeoPoint first = points.get(0);
            if (DistanceUtil.distanceMeters(currentGeoPoint, first) > 30) {
                points.add(0, currentGeoPoint);
            }
        }
        renderRoutePolyline(points, detail.name,
                detail.totalDistanceMeters != null ? detail.totalDistanceMeters : calculatePathDistanceMeters(points),
                detail.estimatedTimeMinutes != null ? detail.estimatedTimeMinutes : estimateRouteMinutes(calculatePathDistanceMeters(points)),
                buildSavedRouteStepText(stops));
    }

    private void renderBackendRoutePlan(@NonNull RoutePlanDto plan, @NonNull String routeName) {
        if (plan.routePoints == null || plan.routePoints.isEmpty()) {
            return;
        }
        List<GeoPoint> points = new ArrayList<>(plan.routePoints.size());
        for (RoutePlanDto.GeoPointDto point : plan.routePoints) {
            points.add(new GeoPoint(point.latitude, point.longitude));
        }
        String steps = "-";
        if (plan.instructions != null && !plan.instructions.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (RoutePlanDto.RouteInstructionDto instruction : plan.instructions) {
                String text = instruction.instruction != null && !instruction.instruction.isEmpty()
                        ? instruction.instruction
                        : instruction.description;
                if (text != null && !text.isEmpty()) {
                    labels.add(text);
                }
            }
            if (!labels.isEmpty()) {
                steps = TextUtils.join(" → ", labels);
            }
        }
        renderRoutePolyline(points, routeName, plan.totalDistanceMeters, plan.estimatedTimeMinutes, steps);
    }

    private void renderRoutePolyline(@NonNull List<GeoPoint> points,
                                     @NonNull String routeName,
                                     double distanceMeters,
                                     int estimatedMinutes,
                                     @NonNull String stepText) {
        renderActiveRoutePolyline(points);
        String modeName = getRouteModeLabel(routeMode);
        tvRouteSummary.setText(getString(R.string.label_estimate_time_mode, modeName, estimatedMinutes, distanceMeters / 1000.0)
                + " · " + routeName);
        if (tvRouteSteps != null) {
            tvRouteSteps.setVisibility(View.GONE);
        }
        if (cardRoutePanel != null) {
            cardRoutePanel.setVisibility(View.VISIBLE);
        }
        LocationDto dest = findLocationById(routedLocationId);
        if (dest != null) {
            presentActiveRouteView(points, dest, stepText);
        } else {
            focusMapOnRoute(points);
        }
    }

    @NonNull
    private String buildSavedRouteStepText(@NonNull List<UserRouteDetailDto.WaypointDto> stops) {
        List<String> labels = new ArrayList<>();
        for (UserRouteDetailDto.WaypointDto stop : stops) {
            if (stop.locationName != null && !stop.locationName.isEmpty()) {
                labels.add(stop.locationName);
            } else {
                labels.add(String.format(Locale.CHINA, "%.4f,%.4f", stop.latitude, stop.longitude));
            }
        }
        return getString(R.string.label_route_steps_format, TextUtils.join(" → ", labels));
    }

    @Nullable
    private LocationDto findLocationById(long locationId) {
        for (LocationDto location : locations) {
            if (location != null && location.id == locationId) {
                return location;
            }
        }
        return null;
    }

    private void toggleAutoGuide() {
        autoGuideEnabled = !autoGuideEnabled;
        updateAutoGuideTile();
        toast(getString(autoGuideEnabled ? R.string.toast_auto_on : R.string.toast_auto_off));
    }

    private void updateAutoGuideTile() {
        tileAutoGuide.setBackgroundResource(autoGuideEnabled ? R.drawable.bg_quick_tile : R.drawable.bg_tool_button);
        tileAutoGuide.setAlpha(autoGuideEnabled ? 1f : 0.75f);
    }

    private void toggleTrack() {
        trackEnabled = !trackEnabled;
        if (trackManager != null) {
            trackManager.setEnabled(trackEnabled);
        }
        updateTrackTile();
        toast(getString(trackEnabled ? R.string.toast_track_on : R.string.toast_track_off));
    }

    private void updateTrackTile() {
        tileTrack.setBackgroundResource(trackEnabled ? R.drawable.bg_quick_tile : R.drawable.bg_tool_button);
        tileTrack.setAlpha(trackEnabled ? 1f : 0.75f);
    }

    private void switchTileLayer() {
        String layerName = campusMapManager.switchTileLayer(this);
        onMapLayerChanged();
        toast(getString(R.string.toast_switched_layer, layerName));
    }

    private void onMapLayerChanged() {
        campusMapManager.onTileLayerChanged(this);
        rebuildLocationDisplayPoints();
        initCampusRouteGraph();
        renderLocationMarkersAndFences();

        if (currentRawGeoPoint != null) {
            currentGeoPoint = gpsToMapPoint(
                    currentRawGeoPoint.getLatitude(),
                    currentRawGeoPoint.getLongitude()
            );
            if (userMarker != null) {
                userMarker.setPosition(CampusMapManager.toLatLng(currentGeoPoint));
            }
        }

        if (selectedLocation != null && currentRawGeoPoint != null) {
            double distance = distanceRawToLocationMeters(currentRawGeoPoint, selectedLocation);
            selectLocation(selectedLocation, distance, false, false);
            return;
        }

        if (reapplyMapCameraLock()) {
            return;
        }

        if (currentGeoPoint != null && currentRawGeoPoint != null && isInCampusSmart(currentRawGeoPoint)) {
            campusMapManager.animateTo(currentGeoPoint);
        }
    }
    private void loadLocations() {
        if (!isUiAlive()) {
            return;
        }
        mapProgress.setVisibility(View.VISIBLE);
        api.locations().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LocationDto>>> call, Response<ApiResponse<List<LocationDto>>> response) {
                runOnUiThreadIfAlive(() -> {
                    mapProgress.setVisibility(View.GONE);
                    ApiResponse<List<LocationDto>> body = response.body();

                    List<LocationDto> normalized = null;
                    backendLocationsLoaded = false;
                    if (response.isSuccessful() && body != null && body.code == 0 && body.data != null) {
                        normalized = sanitizeCampusLocations(body.data);
                        backendLocationsLoaded = true;
                    }

                    if (normalized == null || normalized.isEmpty()) {
                        if (backendLocationsLoaded) {
                            toast(getString(R.string.toast_backend_locations_empty));
                        } else {
                            toast(getString(R.string.toast_backend_locations_failed));
                        }
                        normalized = buildDefaultSwuLocations();
                        updateStatus("地图实时状态：后端无数据，地图使用内置参考点位");
                    } else {
                        updateStatus("地图实时状态：已加载后端景点 " + normalized.size() + " 个");
                    }

                    applyLoadedLocations(normalized);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LocationDto>>> call, Throwable throwable) {
                runOnUiThreadIfAlive(() -> {
                    mapProgress.setVisibility(View.GONE);
                    backendLocationsLoaded = false;
                    toast(getString(R.string.toast_backend_locations_failed));
                    applyLoadedLocations(buildDefaultSwuLocations());
                    updateStatus("地图实时状态：网络失败，地图使用内置参考点位");
                });
            }
        });
    }

    private void applyLoadedLocations(@NonNull List<LocationDto> normalized) {
        if (!isUiAlive()) {
            return;
        }
        locations.clear();
        locations.addAll(normalized);
        locationCoordinateOverrides.clear();
        pendingPoiCoordinateFixes.clear();
        initCampusRouteGraph();
        rebuildLocationDisplayPoints();
        mergeBackendLocationsIntoRouteGraph();
        selectedLocation = null;
        renderLocationMarkersAndFences();
        resolveAmapCoordinatesForMissingOnly();
        refreshNearbyCards();
        updateCampusSnapshotStatus();
        refreshRoutePickerOptions();
        if (!reapplyMapCameraLock() && routePolyline == null && routedLocationId < 0) {
            applyDefaultMapAnchor(true);
        }
    }

    private void restoreSelectedLocationIfPresent(long locationId) {
        if (locationId <= 0 || ROUTE_END_DESTINATION.equals(selectedRouteEndId)) {
            return;
        }
        LocationDto restored = findLocationById(locationId);
        if (restored == null) {
            return;
        }
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, restored);
        selectLocation(restored, distance, false, false, false);
    }

    private void renderLocationMarkersAndFences() {
        if (!isMapReady()) {
            return;
        }
        markerManager.setGeofenceVisible(geofenceVisible);
        markerManager.render(locations, this::resolveDisplayPoint, DEFAULT_RADIUS_METERS);
        if (selectedLocation != null) {
            markerManager.setSelectedLocationId(selectedLocation.id);
        }
    }

    private void showAmapMapPoiDetail(@Nullable Poi poi) {
        if (poi == null || poi.getCoordinate() == null || TextUtils.isEmpty(poi.getName())) {
            return;
        }
        GeoPoint point = new GeoPoint(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
        LocationDto location = buildClickedAmapPoiLocation(poi, point);
        locationCoordinateOverrides.put(location.id, point);
        locationDisplayPoints.put(location.id, point);
        poiRouteNodeHints.put(location.id, findNearestRouteNode(point));
        upsertSearchLocation(location);

        Double distance = currentRawGeoPoint == null ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, location);
        clearRouteIfDestinationChanged(location.id);
        holdUserMapResult();
        selectLocation(location, distance);
        setBottomPanelCollapsed(false, true);
        renderLocationMarkersAndFences();
        poiRouteNodeHints.put(location.id, findNearestRouteNode(point));
        focusLocation(location);
        requestAmapPoiDetailForMapClick(poi, location.id);
    }

    private LocationDto buildClickedAmapPoiLocation(@NonNull Poi poi, @NonNull GeoPoint point) {
        LocationDto content = findBestContentByKeywordV2(poi.getName());
        if (content != null) {
            return content;
        }
        LocationDto dto = new LocationDto();
        dto.id = AMAP_CLICKED_POI_LOCATION_ID;
        dto.name = safeText(poi.getName(), "地图地标");
        dto.category = "高德地图地标";
        dto.address = "高德地图地标";
        dto.latitude = point.getLatitude();
        dto.longitude = point.getLongitude();
        dto.radiusMeters = DEFAULT_RADIUS_METERS;
        dto.description = dto.name + "。该地标来自高德地图，暂无数据库讲解内容。";
        return dto;
    }

    private void requestAmapPoiDetailForMapClick(@NonNull Poi poi, long locationId) {
        if (TextUtils.isEmpty(poi.getPoiId())) {
            return;
        }
        PoiSearch.Query query = new PoiSearch.Query(safeText(poi.getName(), ""), "", "重庆");
        query.setCityLimit(false);
        query.setExtensions(PoiSearch.EXTENSIONS_BASE);
        try {
            PoiSearch search = new PoiSearch(this, query);
            search.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int code) {
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int code) {
                    if (code != AMapException.CODE_AMAP_SUCCESS || poiItem == null || selectedLocation == null
                            || selectedLocation.id != locationId) {
                        return;
                    }
                    updateClickedAmapPoiDetail(locationId, poiItem);
                }
            });
            search.searchPOIIdAsyn(poi.getPoiId());
        } catch (AMapException ignored) {
        }
    }

    private void updateClickedAmapPoiDetail(long locationId, @NonNull PoiItem poiItem) {
        LocationDto location = findLocationById(locationId);
        if (location == null) {
            return;
        }
        boolean isTemporaryAmapPoi = location.id == AMAP_CLICKED_POI_LOCATION_ID;
        if (isTemporaryAmapPoi) {
            location.category = safeText(poiItem.getTypeDes(), location.category);
            location.address = safeText(poiItem.getSnippet(), location.address);
            location.description = location.name + "。该地标来自高德地图，暂无数据库讲解内容。";
        } else if (TextUtils.isEmpty(location.address)) {
            location.address = safeText(poiItem.getSnippet(), location.address);
        }
        if (poiItem.getLatLonPoint() != null) {
            GeoPoint point = toGeoPoint(poiItem.getLatLonPoint());
            if (isTemporaryAmapPoi || !hasPlottableLocation(location)) {
                locationCoordinateOverrides.put(location.id, point);
                locationDisplayPoints.put(location.id, point);
                location.latitude = point.getLatitude();
                location.longitude = point.getLongitude();
            }
            GeoPoint routePoint = resolveDisplayPoint(location);
            poiRouteNodeHints.put(location.id, findNearestRouteNode(
                    routePoint != null ? routePoint : point));
        }
        Double distance = currentRawGeoPoint == null ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, location);
        selectLocation(location, distance, false);
        renderLocationMarkersAndFences();
    }

    /** 仅对缺少有效经纬度的景点做高德 POI 补全；已有后端坐标的不做覆盖。 */
    private void resolveAmapCoordinatesForMissingOnly() {
        for (LocationDto location : locations) {
            if (location == null || location.id >= AMAP_SEARCH_LOCATION_ID_BASE) {
                continue;
            }
            if (hasPlottableLocation(location)) {
                continue;
            }
            requestAmapPoiCoordinateFix(location, false);
        }
    }

    private void redrawGeofences() {
        renderLocationMarkersAndFences();
    }

    private List<LocationDto> sanitizeCampusLocations(@Nullable List<LocationDto> source) {
        List<LocationDto> filtered = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return filtered;
        }

        Set<Long> usedIds = new HashSet<>();
        long generatedId = 100000L;
        for (LocationDto raw : source) {
            if (raw == null) {
                continue;
            }
            if (TextUtils.isEmpty(raw.name)) {
                continue;
            }

            LocationDto item = new LocationDto();
            long id = raw.id;
            if (id <= 0 || usedIds.contains(id)) {
                while (usedIds.contains(generatedId)) {
                    generatedId++;
                }
                id = generatedId++;
            }
            usedIds.add(id);

            item.id = id;
            item.name = safeText(raw.name, "校园讲解点");
            item.category = safeText(raw.category, "校园地点");
            item.address = safeText(raw.address, "西南大学北碚校区");
            item.latitude = isValidLatLng(raw.latitude, raw.longitude) ? raw.latitude : 0;
            item.longitude = isValidLatLng(raw.latitude, raw.longitude) ? raw.longitude : 0;
            item.radiusMeters = raw.radiusMeters > 0 ? raw.radiusMeters : DEFAULT_RADIUS_METERS;
            item.cooldownSeconds = raw.cooldownSeconds > 0 ? raw.cooldownSeconds : 120;
            item.description = safeText(raw.description, item.name + "，西南大学校园讲解点。");
            item.audioUrl = raw.audioUrl;
            item.imageUrl = raw.imageUrl;
            item.imageUrls = raw.imageUrls;
            item.likeCount = raw.likeCount;
            item.liked = raw.liked;
            filtered.add(item);
        }
        return filtered;
    }

    private List<LocationDto> buildDefaultSwuLocations() {
        List<LocationDto> defaults = new ArrayList<>();
        defaults.add(buildLocation(1, "西南大学中心图书馆", "图书馆", 29.8230614, 106.4204318,
                "重庆市北碚区天生路2号 西南大学中心图书馆", "这里是西南大学中心图书馆，拥有丰富的纸质与电子资源。"));
        defaults.add(buildLocation(2, "25教学楼", "教学楼", 29.8251130, 106.4202302,
                "西南大学北碚校区 25教学楼", "25教学楼是校园重要教学区域。"));
        defaults.add(buildLocation(3, "地理科学学院", "学院", 29.8241302, 106.4189186,
                "西南大学地理科学学院", "地理科学学院承担地理学相关教学与科研任务。"));
        defaults.add(buildLocation(4, "计算机与信息科学学院学术报告厅", "学院", 29.8244300, 106.4184100,
                "西南大学计算机与信息科学学院", "学术报告厅是举办讲座与论坛的重要场所。"));
        defaults.add(buildLocation(5, "光华楼", "行政楼", 29.8245400, 106.4179620,
                "西南大学光华楼", "光华楼位于校内核心区域。"));
        defaults.add(buildLocation(6, "第三运动场", "运动场", 29.8262060, 106.4185040,
                "西南大学第三运动场", "第三运动场是开展体育活动的重要场地。"));
        defaults.add(buildLocation(7, "研究生院", "学院", 29.8208890, 106.4176230,
                "西南大学研究生院", "研究生院负责研究生培养与管理工作。"));
        defaults.add(buildLocation(8, "西南大学南门", "校门", 29.8206700, 106.4236000,
                "西南大学南门（北碚校区）", "南门是游客与师生常用出入口之一。"));
        return defaults;
    }

    private LocationDto buildLocation(long id, String name, String category, double lat, double lng, String address, String desc) {
        LocationDto dto = new LocationDto();
        dto.id = id;
        dto.name = name;
        dto.category = category;
        dto.address = address;
        dto.latitude = lat;
        dto.longitude = lng;
        dto.radiusMeters = DEFAULT_RADIUS_METERS;
        dto.cooldownSeconds = 120;
        dto.description = desc;
        dto.audioUrl = null;
        dto.imageUrl = null;
        return dto;
    }

    private boolean isCampusPoint(double lat, double lng) {
        return CampusMapManager.isCampusPoint(lat, lng);
    }

    private boolean isValidLatLng(double lat, double lng) {
        return Math.abs(lat) > 0.0001
                && Math.abs(lng) > 0.0001
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void refreshNearbyCards() {
        if (currentRawGeoPoint == null || locations.isEmpty()) {
            nearbyAdapter.submit(new ArrayList<>());
            return;
        }

        List<NearbyLocationAdapter.NearbyItem> nearby = new ArrayList<>();
        for (LocationDto location : locations) {
            if (resolveDisplayPoint(location) == null) {
                continue;
            }
            double distance = distanceRawToLocationMeters(currentRawGeoPoint, location);
            nearby.add(new NearbyLocationAdapter.NearbyItem(location, distance));
        }
        nearby.sort(Comparator.comparingDouble(it -> it.distance));
        if (nearby.size() > 8) {
            nearby = nearby.subList(0, 8);
        }
        nearbyAdapter.submit(nearby);
    }

    private void selectLocation(LocationDto location, @Nullable Double distance) {
        selectLocation(location, distance, true, false, false);
    }

    private void selectLocation(LocationDto location, @Nullable Double distance, boolean autoExpandPanel) {
        selectLocation(location, distance, autoExpandPanel, true, false);
    }

    private void selectLocation(LocationDto location,
                                @Nullable Double distance,
                                boolean autoExpandPanel,
                                boolean allowCameraFocus) {
        selectLocation(location, distance, autoExpandPanel, allowCameraFocus, false);
    }

    private void selectLocation(LocationDto location,
                                @Nullable Double distance,
                                boolean autoExpandPanel,
                                boolean allowCameraFocus,
                                boolean syncRouteEnd) {
        selectedLocation = location;
        if (autoExpandPanel && bottomPanelCollapsed) {
            setBottomPanelCollapsed(false, true);
        }
        tvLocationName.setText(location.name);
        tvDescription.setText(buildLocationDescription(location));

        if (distance != null && com.example.gps.util.DistanceFormat.isUsable(distance)) {
            tvDistance.setText(com.example.gps.util.DistanceFormat.formatWithLabel(this, distance));
            tvDistance.setVisibility(View.VISIBLE);
        } else {
            tvDistance.setText("-");
        }
        updateGuidePointLabel();
    }

    private String buildLocationDescription(LocationDto location) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(location.category)) {
            builder.append("分类：").append(location.category).append("  ");
        }
        if (!TextUtils.isEmpty(location.address)) {
            builder.append("地址：").append(location.address).append('\n');
        }
        if (!TextUtils.isEmpty(location.description)) {
            builder.append(location.description);
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private void updateStatus(String text) {
        // 顶部状态条已移除，保留方法以免改动多处调用
    }

    private void searchLocationByName(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            toast(getString(R.string.toast_input_place));
            return;
        }
        holdUserMapResult();
        final long sessionId = ++searchSessionId;
        final String trimmed = keyword.trim();
        new Thread(() -> {
            ContentMatch localMatch = findBestContentMatch(trimmed);
            runOnUiThreadIfAlive(() -> {
                if (sessionId != searchSessionId) {
                    return;
                }
                if (localMatch != null
                        && localMatch.score <= 2
                        && resolveDisplayPoint(localMatch.location) != null) {
                    presentCampusSpotSearchResult(localMatch.location);
                    return;
                }
                searchAmapPoiForKeywordV2(trimmed, sessionId);
            });
        }, "map-local-search").start();
    }

    private void searchAmapPoiForKeywordV2(@NonNull String keyword, long sessionId) {
        if (sessionId != searchSessionId) {
            return;
        }
        tvRouteSummary.setText("正在使用高德搜索地点...");
        tvRouteSteps.setText("");
        lastAmapPoiSearchErrorCode = AMapException.CODE_AMAP_SUCCESS;
        List<String> queries = buildAmapSearchQueriesV2(keyword);
        searchAmapTipAttemptV2(keyword, queries, 0, new ArrayList<>(), sessionId);
    }

    private void searchAmapPoiForKeywordV2(@NonNull String keyword) {
        searchAmapPoiForKeywordV2(keyword, searchSessionId);
    }

    private void searchAmapTipAttemptV2(@NonNull String keyword,
                                        @NonNull List<String> queries,
                                        int index,
                                        @NonNull List<Tip> collected,
                                        long sessionId) {
        if (sessionId != searchSessionId) {
            return;
        }
        if (index >= queries.size()) {
            Tip best = findBestAmapTipByKeywordV2(keyword, collected);
            if (best != null && best.getPoint() != null) {
                showAmapTipSearchResultV2(keyword, best);
                return;
            }
            Tip poiIdTip = findBestAmapTipPoiIdByKeywordV2(keyword, collected);
            if (poiIdTip != null) {
                searchAmapPoiIdFromTipV2(keyword, queries, poiIdTip, sessionId);
                return;
            }
            searchAmapPoiAttemptV2(keyword, queries, 0, new ArrayList<>(), sessionId);
            return;
        }

        InputtipsQuery query = new InputtipsQuery(queries.get(index), "重庆");
        query.setCityLimit(false);
        query.setLocation(new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG));
        try {
            Inputtips tips = new Inputtips(this, (list, code) -> {
                if (sessionId != searchSessionId) {
                    return;
                }
                if (code == AMapException.CODE_AMAP_SUCCESS && list != null) {
                    collected.addAll(list);
                }
                searchAmapTipAttemptV2(keyword, queries, index + 1, collected, sessionId);
            });
            tips.setQuery(query);
            tips.requestInputtipsAsyn();
        } catch (AMapException e) {
            searchAmapTipAttemptV2(keyword, queries, index + 1, collected, sessionId);
        }
    }

    private void searchAmapPoiIdFromTipV2(@NonNull String keyword,
                                          @NonNull List<String> queries,
                                          @NonNull Tip tip,
                                          long sessionId) {
        if (sessionId != searchSessionId) {
            return;
        }
        String poiId = tip.getPoiID();
        if (TextUtils.isEmpty(poiId)) {
            searchAmapPoiAttemptV2(keyword, queries, 0, new ArrayList<>(), sessionId);
            return;
        }
        PoiSearch.Query query = new PoiSearch.Query(safeText(tip.getName(), keyword), "", "重庆");
        query.setCityLimit(false);
        query.setExtensions(PoiSearch.EXTENSIONS_BASE);
        try {
            PoiSearch search = new PoiSearch(this, query);
            search.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int code) {
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int code) {
                    if (sessionId != searchSessionId) {
                        return;
                    }
                    if (code == AMapException.CODE_AMAP_SUCCESS
                            && poiItem != null
                            && poiItem.getLatLonPoint() != null
                            && isStrongAmapPoiMatchV2(keyword, poiItem)) {
                        showAmapPoiSearchResultV2(keyword, poiItem);
                        return;
                    }
                    if (code != AMapException.CODE_AMAP_SUCCESS) {
                        lastAmapPoiSearchErrorCode = code;
                    }
                    searchAmapPoiAttemptV2(keyword, queries, 0, new ArrayList<>(), sessionId);
                }
            });
            search.searchPOIIdAsyn(poiId);
        } catch (AMapException e) {
            lastAmapPoiSearchErrorCode = e.getErrorCode();
            searchAmapPoiAttemptV2(keyword, queries, 0, new ArrayList<>(), sessionId);
        }
    }

    private void searchAmapPoiAttemptV2(@NonNull String keyword,
                                        @NonNull List<String> queries,
                                        int index,
                                        @NonNull List<PoiItem> collected,
                                        long sessionId) {
        if (sessionId != searchSessionId) {
            return;
        }
        if (index >= queries.size()) {
            PoiItem best = findBestAmapPoiByKeywordV2(keyword, collected);
            if (best != null && best.getLatLonPoint() != null) {
                showAmapPoiSearchResultV2(keyword, best);
                return;
            }
            searchAmapGeocodeAttemptV2(keyword, queries, 0, new ArrayList<>(), sessionId);
            return;
        }

        PoiSearch.Query query = new PoiSearch.Query(queries.get(index), "", "重庆");
        query.setPageSize(25);
        query.setPageNum(0);
        query.setCityLimit(false);
        query.setDistanceSort(true);
        query.setLocation(new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG));
        query.setExtensions(PoiSearch.EXTENSIONS_BASE);
        try {
            PoiSearch search = new PoiSearch(this, query);
            if (index < queries.size() - 1) {
                search.setBound(new PoiSearch.SearchBound(
                        new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG),
                        (int) AMAP_POI_ACCEPT_RADIUS_METERS,
                        true));
            }
            search.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int code) {
                    if (sessionId != searchSessionId) {
                        return;
                    }
                    if (code == AMapException.CODE_AMAP_SUCCESS && result != null && result.getPois() != null) {
                        collected.addAll(result.getPois());
                    } else if (code != AMapException.CODE_AMAP_SUCCESS) {
                        lastAmapPoiSearchErrorCode = code;
                    }
                    searchAmapPoiAttemptV2(keyword, queries, index + 1, collected, sessionId);
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int code) {
                }
            });
            search.searchPOIAsyn();
        } catch (AMapException e) {
            lastAmapPoiSearchErrorCode = e.getErrorCode();
            searchAmapPoiAttemptV2(keyword, queries, index + 1, collected, sessionId);
        }
    }

    private void searchAmapGeocodeAttemptV2(@NonNull String keyword,
                                            @NonNull List<String> queries,
                                            int index,
                                            @NonNull List<GeocodeAddress> collected,
                                            long sessionId) {
        if (sessionId != searchSessionId) {
            return;
        }
        if (index >= queries.size()) {
            GeocodeAddress best = findBestAmapGeocodeByKeywordV2(keyword, collected);
            if (best != null && best.getLatLonPoint() != null) {
                showAmapGeocodeSearchResultV2(keyword, best);
                return;
            }
            showAmapPoiSearchFailureToast();
            return;
        }

        try {
            GeocodeSearch search = new GeocodeSearch(this);
            search.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
                @Override
                public void onRegeocodeSearched(RegeocodeResult result, int code) {
                }

                @Override
                public void onGeocodeSearched(GeocodeResult result, int code) {
                    if (sessionId != searchSessionId) {
                        return;
                    }
                    if (code == AMapException.CODE_AMAP_SUCCESS
                            && result != null
                            && result.getGeocodeAddressList() != null) {
                        collected.addAll(result.getGeocodeAddressList());
                    } else if (code != AMapException.CODE_AMAP_SUCCESS) {
                        lastAmapPoiSearchErrorCode = code;
                    }
                    searchAmapGeocodeAttemptV2(keyword, queries, index + 1, collected, sessionId);
                }
            });
            search.getFromLocationNameAsyn(new GeocodeQuery(queries.get(index), "重庆"));
        } catch (AMapException e) {
            lastAmapPoiSearchErrorCode = e.getErrorCode();
            searchAmapGeocodeAttemptV2(keyword, queries, index + 1, collected, sessionId);
        }
    }

    private void showAmapPoiSearchFailureToast() {
        if (lastAmapPoiSearchErrorCode == AMapException.CODE_AMAP_USERKEY_PLAT_NOMATCH) {
            toast("高德搜索失败：Key 与包名/SHA1 不匹配。控制台创建 Android Key，填写 com.example.gps 与调试 SHA1");
            return;
        }
        if (lastAmapPoiSearchErrorCode == 1008) {
            toast("高德搜索失败(1008)：请在控制台开通「搜索」服务，并配置包名 com.example.gps 与调试 SHA1");
            return;
        }
        if (lastAmapPoiSearchErrorCode != AMapException.CODE_AMAP_SUCCESS) {
            toast("高德搜索失败，错误码：" + lastAmapPoiSearchErrorCode);
            return;
        }
        toast(getString(R.string.toast_search_not_on_campus));
    }

    private List<String> buildAmapSearchQueriesV2(@NonNull String keyword) {
        String trimmed = keyword.trim();
        List<String> queries = new ArrayList<>();
        for (String alias : buildSearchAliases(trimmed)) {
            addUniqueQueryV2(queries, alias);
            addUniqueQueryV2(queries, buildAmapCampusQuery(alias));
            addUniqueQueryV2(queries, "重庆" + buildAmapCampusQuery(alias));
        }
        LocationDto content = findBestContentByKeywordV2(trimmed);
        if (content != null && !TextUtils.isEmpty(content.name)) {
            for (String alias : buildSearchAliases(content.name)) {
                addUniqueQueryV2(queries, alias);
                addUniqueQueryV2(queries, buildAmapCampusQuery(alias));
                addUniqueQueryV2(queries, "重庆" + buildAmapCampusQuery(alias));
            }
        }
        if (queries.size() > MAX_AMAP_SEARCH_QUERIES) {
            return new ArrayList<>(queries.subList(0, MAX_AMAP_SEARCH_QUERIES));
        }
        return queries;
    }

    private void addUniqueQueryV2(@NonNull List<String> queries, @Nullable String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) {
            return;
        }
        String normalized = normalizeSearchText(value);
        for (String existing : queries) {
            if (normalizeSearchText(existing).equals(normalized)) {
                return;
            }
        }
        queries.add(value);
    }

    private List<String> buildSearchAliases(@NonNull String keyword) {
        List<String> aliases = new ArrayList<>();
        addUniqueQueryV2(aliases, keyword);

        String buildingNumber = extractTeachingBuildingNumber(keyword);
        if (!TextUtils.isEmpty(buildingNumber)) {
            addUniqueQueryV2(aliases, buildingNumber + "号教学楼");
            addUniqueQueryV2(aliases, buildingNumber + "教学楼");
            addUniqueQueryV2(aliases, "第" + buildingNumber + "教学楼");
            addUniqueQueryV2(aliases, "第" + buildingNumber + "号教学楼");
            addUniqueQueryV2(aliases, buildingNumber + "号楼");
            addUniqueQueryV2(aliases, buildingNumber + "号教");
            addUniqueQueryV2(aliases, buildingNumber + "教");
            addUniqueQueryV2(aliases, "西南大学" + buildingNumber + "号教学楼");
            addUniqueQueryV2(aliases, "西南大学" + buildingNumber + "教学楼");
            addUniqueQueryV2(aliases, "西南大学北碚校区" + buildingNumber + "号教学楼");
            addUniqueQueryV2(aliases, "西南大学北碚校区" + buildingNumber + "教学楼");
            addUniqueQueryV2(aliases, "西南大学(北碚校区)" + buildingNumber + "号教学楼");
            addUniqueQueryV2(aliases, "西南大学(北碚校区)" + buildingNumber + "教学楼");
            addUniqueQueryV2(aliases, buildingNumber + "号教学楼 西南大学");
            addUniqueQueryV2(aliases, buildingNumber + "号教学楼 西南大学北碚校区");
        }

        return aliases;
    }

    private Set<String> buildNormalizedSearchAliases(@NonNull String keyword) {
        Set<String> aliases = new HashSet<>();
        for (String alias : buildSearchAliases(keyword)) {
            String normalized = normalizeSearchText(alias);
            if (!TextUtils.isEmpty(normalized)) {
                aliases.add(normalized);
                aliases.add(normalizeSearchText(buildAmapCampusQuery(alias)));
            }
        }
        return aliases;
    }

    @Nullable
    private String extractTeachingBuildingNumber(@Nullable String keyword) {
        String normalized = normalizeSearchText(keyword).replace("第", "");
        if (TextUtils.isEmpty(normalized)) {
            return null;
        }
        boolean looksLikeBuilding = normalized.contains("教学楼")
                || normalized.contains("号楼")
                || normalized.contains("号教")
                || normalized.endsWith("教")
                || normalized.endsWith("楼")
                || normalized.endsWith("号");
        if (!looksLikeBuilding) {
            return null;
        }
        Matcher matcher = TEACHING_BUILDING_NUMBER_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Nullable
    private String extractStrictTeachingBuildingNumber(@Nullable String text) {
        String normalized = normalizeSearchText(text).replace("第", "");
        if (TextUtils.isEmpty(normalized)) {
            return null;
        }
        Matcher matcher = STRICT_TEACHING_BUILDING_NUMBER_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int scoreNormalizedTextAgainstAliases(@Nullable String normalizedText,
                                                  @NonNull Set<String> aliases) {
        if (TextUtils.isEmpty(normalizedText) || aliases.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        for (String alias : aliases) {
            if (normalizedText.equals(alias)) {
                return 0;
            }
        }
        for (String alias : aliases) {
            if (TextUtils.isEmpty(alias)) {
                continue;
            }
            if (normalizedText.contains(alias)) {
                return 1;
            }
            if (normalizedText.length() >= 3
                    && alias.contains(normalizedText)
                    && !isGenericCampusText(normalizedText)) {
                return 1;
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean containsAnySearchAlias(@Nullable String normalizedText,
                                           @NonNull Set<String> aliases) {
        return scoreNormalizedTextAgainstAliases(normalizedText, aliases) != Integer.MAX_VALUE;
    }

    private boolean hasSameTeachingBuildingNumber(@NonNull String keyword, @Nullable String text) {
        String expected = extractTeachingBuildingNumber(keyword);
        String actual = extractStrictTeachingBuildingNumber(text);
        return !TextUtils.isEmpty(expected) && expected.equals(actual);
    }

    private boolean hasConflictingTeachingBuildingNumber(@NonNull String keyword, @Nullable String text) {
        String expected = extractTeachingBuildingNumber(keyword);
        if (TextUtils.isEmpty(expected)) {
            return false;
        }
        String actual = extractStrictTeachingBuildingNumber(text);
        return !TextUtils.isEmpty(actual) && !expected.equals(actual);
    }

    private boolean isGenericCampusText(@NonNull String normalizedText) {
        return "西南大学".equals(normalizedText)
                || "重庆西南大学".equals(normalizedText)
                || "西南大学北碚校区".equals(normalizedText)
                || "西南大学校区".equals(normalizedText);
    }

    private boolean isTeachingBuildingSearch(@NonNull String keyword) {
        return !TextUtils.isEmpty(extractTeachingBuildingNumber(keyword));
    }

    private boolean isAcceptableAmapPoiForKeyword(@NonNull String keyword,
                                                  double lat,
                                                  double lng,
                                                  double distanceFromCampusCenter) {
        if (isTeachingBuildingSearch(keyword)) {
            return CampusMapManager.isInCampusRect(lat, lng)
                    || (isCampusPoint(lat, lng) && distanceFromCampusCenter <= CAMPUS_INSIDE_RADIUS_METERS);
        }
        return distanceFromCampusCenter <= AMAP_POI_ACCEPT_RADIUS_METERS;
    }

    @Nullable
    private Tip findBestAmapTipByKeywordV2(@NonNull String keyword, @NonNull List<Tip> tips) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        Tip best = null;
        int bestScore = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (Tip tip : tips) {
            if (tip == null || tip.getPoint() == null) {
                continue;
            }
            double lat = tip.getPoint().getLatitude();
            double lng = tip.getPoint().getLongitude();
            double distance = calculateDistanceMeters(
                    CampusMapManager.CAMPUS_CENTER_LAT,
                    CampusMapManager.CAMPUS_CENTER_LNG,
                    lat,
                    lng);
            if (!isAcceptableAmapPoiForKeyword(keyword, lat, lng, distance)) {
                continue;
            }
            String name = normalizeSearchText(tip.getName());
            String address = normalizeSearchText(tip.getAddress());
            String district = normalizeSearchText(tip.getDistrict());
            if (hasConflictingTeachingBuildingNumber(keyword, tip.getName())
                    || hasConflictingTeachingBuildingNumber(keyword, tip.getAddress())) {
                continue;
            }
            String haystack = name + "|" + address + "|" + district;
            int score = Integer.MAX_VALUE;
            int nameScore = scoreNormalizedTextAgainstAliases(name, aliases);
            if (nameScore == 0) {
                score = 0;
            } else if (nameScore == 1) {
                score = 1;
            } else if (containsAnySearchAlias(haystack, aliases)) {
                score = 2;
            } else if (hasSameTeachingBuildingNumber(keyword, tip.getName())
                    || hasSameTeachingBuildingNumber(keyword, tip.getAddress())) {
                score = 3;
            }
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            if (score < bestScore || (score == bestScore && distance < bestDistance)) {
                best = tip;
                bestScore = score;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Nullable
    private Tip findBestAmapTipPoiIdByKeywordV2(@NonNull String keyword, @NonNull List<Tip> tips) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        boolean teachingSearch = isTeachingBuildingSearch(keyword);
        Tip best = null;
        int bestScore = Integer.MAX_VALUE;
        int bestCampusScore = Integer.MAX_VALUE;
        for (Tip tip : tips) {
            if (tip == null || TextUtils.isEmpty(tip.getPoiID())) {
                continue;
            }
            if (hasConflictingTeachingBuildingNumber(keyword, tip.getName())
                    || hasConflictingTeachingBuildingNumber(keyword, tip.getAddress())) {
                continue;
            }
            String name = normalizeSearchText(tip.getName());
            String address = normalizeSearchText(tip.getAddress());
            String district = normalizeSearchText(tip.getDistrict());
            String haystack = name + "|" + address + "|" + district;
            if (teachingSearch
                    && !hasSameTeachingBuildingNumber(keyword, tip.getName())
                    && !hasSameTeachingBuildingNumber(keyword, tip.getAddress())) {
                continue;
            }
            int score = Integer.MAX_VALUE;
            int nameScore = scoreNormalizedTextAgainstAliases(name, aliases);
            if (nameScore == 0) {
                score = 0;
            } else if (nameScore == 1) {
                score = 1;
            } else if (containsAnySearchAlias(haystack, aliases)) {
                score = 2;
            } else if (hasSameTeachingBuildingNumber(keyword, tip.getName())
                    || hasSameTeachingBuildingNumber(keyword, tip.getAddress())) {
                score = 3;
            }
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            int campusScore = haystack.contains(normalizeSearchText("西南大学")) ? 0
                    : haystack.contains(normalizeSearchText("北碚")) ? 1
                    : haystack.contains(normalizeSearchText("重庆")) ? 2
                    : 3;
            if (score < bestScore || (score == bestScore && campusScore < bestCampusScore)) {
                best = tip;
                bestScore = score;
                bestCampusScore = campusScore;
            }
        }
        return best;
    }

    /** 数据库景点：仅用后端 GCJ 坐标展示围栏/Marker；临时高德 POI 才写入覆盖坐标。 */
    private void applyAmapSearchResult(@NonNull LocationDto location, @NonNull GeoPoint amapPoint) {
        if (isDatabaseCampusSpot(location)) {
            refreshPoiRouteNodeHint(location);
            selectRouteEndCandidate(location);
            presentCampusSpotSearchResult(location);
            return;
        }
        locationCoordinateOverrides.put(location.id, amapPoint);
        locationDisplayPoints.put(location.id, amapPoint);
        location.latitude = amapPoint.getLatitude();
        location.longitude = amapPoint.getLongitude();
        poiRouteNodeHints.put(location.id, findNearestRouteNode(amapPoint));
        upsertSearchLocation(location);
        refreshRoutePickerOptions();
        selectRouteEndCandidate(location);
        presentNonCampusSearchResult(location, amapPoint);
    }

    private void refreshPoiRouteNodeHint(@NonNull LocationDto location) {
        GeoPoint display = resolveDisplayPoint(location);
        if (display != null) {
            poiRouteNodeHints.put(location.id, findNearestRouteNode(display));
        }
    }

    private void showAmapTipSearchResultV2(@NonNull String keyword, @NonNull Tip tip) {
        if (tip.getPoint() == null) {
            return;
        }
        GeoPoint point = toGeoPoint(tip.getPoint());
        if (!isSearchPointOnCampus(point)) {
            toast(getString(R.string.toast_search_not_on_campus));
            return;
        }
        LocationDto location = mergeAmapTipWithContent(keyword, tip);
        applyAmapSearchResult(location, point);
    }

    private LocationDto mergeAmapTipWithContent(@NonNull String keyword, @NonNull Tip tip) {
        LocationDto content = findBestContentForAmapTip(keyword, tip);
        if (content != null) {
            if (!TextUtils.isEmpty(tip.getName())) {
                content.name = tip.getName();
            }
            content.address = safeText(tip.getAddress(), content.address);
            return content;
        }
        LocationDto dto = new LocationDto();
        dto.id = allocateAmapSearchLocationId();
        dto.name = safeText(tip.getName(), keyword);
        dto.category = "高德地点";
        dto.address = safeText(tip.getAddress(), safeText(tip.getDistrict(), "西南大学"));
        if (tip.getPoint() != null) {
            dto.latitude = tip.getPoint().getLatitude();
            dto.longitude = tip.getPoint().getLongitude();
        }
        dto.radiusMeters = DEFAULT_RADIUS_METERS;
        dto.description = dto.name + "。该地点坐标来自高德搜索，暂无数据库讲解内容。";
        return dto;
    }

    @Nullable
    private LocationDto findBestContentForAmapTip(@NonNull String keyword, @NonNull Tip tip) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        aliases.addAll(buildNormalizedSearchAliases(safeText(tip.getName(), "")));
        String address = normalizeSearchText(tip.getAddress());
        LocationDto best = null;
        int bestScore = Integer.MAX_VALUE;
        for (LocationDto location : locations) {
            if (location == null || location.id >= AMAP_SEARCH_LOCATION_ID_BASE) {
                continue;
            }
            String name = normalizeSearchText(location.name);
            if (hasConflictingTeachingBuildingNumber(keyword, location.name)
                    || hasConflictingTeachingBuildingNumber(keyword, location.address)) {
                continue;
            }
            int score = scoreNormalizedTextAgainstAliases(name, aliases);
            if (score == Integer.MAX_VALUE && containsAnySearchAlias(address, buildNormalizedSearchAliases(location.name))) {
                score = 2;
            }
            if (score < bestScore) {
                best = location;
                bestScore = score;
            }
        }
        return bestScore == Integer.MAX_VALUE ? null : best;
    }

    private void showAmapPoiSearchResultV2(@NonNull String keyword, @NonNull PoiItem poi) {
        if (poi.getLatLonPoint() == null) {
            toast(getString(R.string.toast_search_not_on_campus));
            return;
        }
        GeoPoint point = toGeoPoint(poi.getLatLonPoint());
        if (!isSearchPointOnCampus(point)) {
            toast(getString(R.string.toast_search_not_on_campus));
            return;
        }
        LocationDto location = mergeAmapPoiWithContent(keyword, poi);
        applyAmapSearchResult(location, point);
    }

    private void showAmapGeocodeSearchResultV2(@NonNull String keyword, @NonNull GeocodeAddress address) {
        if (address.getLatLonPoint() == null) {
            toast(getString(R.string.toast_search_not_on_campus));
            return;
        }
        GeoPoint point = toGeoPoint(address.getLatLonPoint());
        if (!isSearchPointOnCampus(point)) {
            toast(getString(R.string.toast_search_not_on_campus));
            return;
        }
        LocationDto location = mergeAmapGeocodeWithContent(keyword, address);
        applyAmapSearchResult(location, point);
    }

    @Nullable
    private PoiItem findBestAmapPoiByKeywordV2(@NonNull String keyword, @NonNull List<PoiItem> pois) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        PoiItem best = null;
        int bestScore = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (PoiItem item : pois) {
            if (item == null || item.getLatLonPoint() == null) {
                continue;
            }
            double lat = item.getLatLonPoint().getLatitude();
            double lng = item.getLatLonPoint().getLongitude();
            double distance = calculateDistanceMeters(
                    CampusMapManager.CAMPUS_CENTER_LAT,
                    CampusMapManager.CAMPUS_CENTER_LNG,
                    lat,
                    lng);
            if (!isAcceptableAmapPoiForKeyword(keyword, lat, lng, distance)) {
                continue;
            }
            String title = normalizeSearchText(item.getTitle());
            String snippet = normalizeSearchText(item.getSnippet());
            if (hasConflictingTeachingBuildingNumber(keyword, item.getTitle())
                    || hasConflictingTeachingBuildingNumber(keyword, item.getSnippet())) {
                continue;
            }
            String haystack = title + "|" + snippet + "|" + normalizeSearchText(item.getTypeDes());
            int score = Integer.MAX_VALUE;
            int titleScore = scoreNormalizedTextAgainstAliases(title, aliases);
            if (titleScore == 0) {
                score = 0;
            } else if (titleScore == 1) {
                score = 1;
            } else if (containsAnySearchAlias(haystack, aliases)) {
                score = 2;
            } else if (hasSameTeachingBuildingNumber(keyword, item.getTitle())
                    || hasSameTeachingBuildingNumber(keyword, item.getSnippet())) {
                score = 3;
            }
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            if (score < bestScore || (score == bestScore && distance < bestDistance)) {
                best = item;
                bestScore = score;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Nullable
    private GeocodeAddress findBestAmapGeocodeByKeywordV2(@NonNull String keyword,
                                                          @NonNull List<GeocodeAddress> addresses) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        boolean teachingSearch = isTeachingBuildingSearch(keyword);
        GeocodeAddress best = null;
        int bestScore = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (GeocodeAddress address : addresses) {
            if (address == null || address.getLatLonPoint() == null) {
                continue;
            }
            double lat = address.getLatLonPoint().getLatitude();
            double lng = address.getLatLonPoint().getLongitude();
            double distance = calculateDistanceMeters(
                    CampusMapManager.CAMPUS_CENTER_LAT,
                    CampusMapManager.CAMPUS_CENTER_LNG,
                    lat,
                    lng);
            if (!isAcceptableAmapPoiForKeyword(keyword, lat, lng, distance)) {
                continue;
            }
            String formatted = normalizeSearchText(address.getFormatAddress());
            if (hasConflictingTeachingBuildingNumber(keyword, address.getFormatAddress())) {
                continue;
            }
            int score = Integer.MAX_VALUE;
            if (teachingSearch) {
                if (hasSameTeachingBuildingNumber(keyword, address.getFormatAddress())) {
                    score = 0;
                }
            } else {
                int addressScore = scoreNormalizedTextAgainstAliases(formatted, aliases);
                if (addressScore <= 1) {
                    score = addressScore;
                } else if (containsAnySearchAlias(formatted, aliases)) {
                    score = 2;
                }
            }
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            if (score < bestScore || (score == bestScore && distance < bestDistance)) {
                best = address;
                bestScore = score;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isStrongAmapPoiMatchV2(@NonNull String keyword, @NonNull PoiItem item) {
        if (item.getLatLonPoint() == null) {
            return false;
        }
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        String title = normalizeSearchText(item.getTitle());
        String snippet = normalizeSearchText(item.getSnippet());
        String haystack = title + "|" + snippet + "|" + normalizeSearchText(item.getTypeDes());
        double distance = calculateDistanceMeters(
                CampusMapManager.CAMPUS_CENTER_LAT,
                CampusMapManager.CAMPUS_CENTER_LNG,
                item.getLatLonPoint().getLatitude(),
                item.getLatLonPoint().getLongitude());
        return isAcceptableAmapPoiForKeyword(keyword,
                item.getLatLonPoint().getLatitude(),
                item.getLatLonPoint().getLongitude(),
                distance)
                && !hasConflictingTeachingBuildingNumber(keyword, item.getTitle())
                && !hasConflictingTeachingBuildingNumber(keyword, item.getSnippet())
                && (scoreNormalizedTextAgainstAliases(title, aliases) <= 1
                || containsAnySearchAlias(haystack, aliases)
                || hasSameTeachingBuildingNumber(keyword, item.getTitle())
                || hasSameTeachingBuildingNumber(keyword, item.getSnippet()));
    }

    @Nullable
    private LocationDto findBestContentByKeywordV2(@NonNull String keyword) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        LocationDto best = null;
        int bestScore = Integer.MAX_VALUE;
        for (LocationDto location : locations) {
            if (location == null || location.id >= AMAP_SEARCH_LOCATION_ID_BASE) {
                continue;
            }
            String name = normalizeSearchText(location.name);
            String category = normalizeSearchText(location.category);
            String address = normalizeSearchText(location.address);
            if (hasConflictingTeachingBuildingNumber(keyword, location.name)
                    || hasConflictingTeachingBuildingNumber(keyword, location.address)) {
                continue;
            }
            int score = Integer.MAX_VALUE;
            int nameScore = scoreNormalizedTextAgainstAliases(name, aliases);
            if (nameScore == 0) {
                score = 0;
            } else if (nameScore == 1) {
                score = 1;
            } else if (containsAnySearchAlias(address, aliases)) {
                score = 2;
            } else if (containsAnySearchAlias(category, aliases)) {
                score = 3;
            }
            if (score < bestScore) {
                best = location;
                bestScore = score;
            }
        }
        return best;
    }

    @Nullable
    private LocationDto findBestCachedContentLocationV2(@NonNull String keyword) {
        LocationDto content = findBestContentByKeywordV2(keyword);
        if (content != null && resolveDisplayPoint(content) != null) {
            return content;
        }
        return null;
    }

    private void searchAmapPoiForKeyword(@NonNull String keyword) {
        tvRouteSummary.setText("正在使用高德搜索地点...");
        tvRouteSteps.setText("");
        PoiSearch.Query query = new PoiSearch.Query(buildAmapCampusQuery(keyword), "", "重庆");
        query.setPageSize(20);
        query.setPageNum(0);
        query.setCityLimit(false);
        query.setDistanceSort(true);
        query.setLocation(new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG));
        query.setExtensions(PoiSearch.EXTENSIONS_BASE);
        try {
            PoiSearch search = new PoiSearch(this, query);
            search.setBound(new PoiSearch.SearchBound(
                    new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG),
                    5000,
                    true));
            search.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int code) {
                    if (code != AMapException.CODE_AMAP_SUCCESS || result == null || result.getPois() == null) {
                        toast(getString(R.string.toast_place_not_found));
                        return;
                    }
                    PoiItem poi = findBestAmapPoiByKeyword(keyword, result.getPois());
                    if (poi == null || poi.getLatLonPoint() == null) {
                        toast(getString(R.string.toast_place_not_found));
                        return;
                    }
                    LocationDto location = mergeAmapPoiWithContent(keyword, poi);
                    GeoPoint point = toGeoPoint(poi.getLatLonPoint());
                    applyAmapSearchResult(location, point);
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int code) {
                }
            });
            search.searchPOIAsyn();
        } catch (AMapException e) {
            toast(getString(R.string.toast_place_not_found));
        }
    }

    private String buildAmapCampusQuery(@NonNull String keyword) {
        String trimmed = keyword.trim();
        return normalizeSearchText(trimmed).contains(normalizeSearchText("西南大学"))
                ? trimmed
                : "西南大学" + trimmed;
    }

    @Nullable
    private PoiItem findBestAmapPoiByKeyword(@NonNull String keyword, @NonNull List<PoiItem> pois) {
        String normalizedKey = normalizeSearchText(keyword);
        PoiItem best = null;
        int bestScore = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (PoiItem item : pois) {
            if (item == null || item.getLatLonPoint() == null) {
                continue;
            }
            double lat = item.getLatLonPoint().getLatitude();
            double lng = item.getLatLonPoint().getLongitude();
            if (!isCampusPoint(lat, lng)) {
                continue;
            }
            String title = normalizeSearchText(item.getTitle());
            String snippet = normalizeSearchText(item.getSnippet());
            if (hasConflictingTeachingBuildingNumber(keyword, item.getTitle())
                    || hasConflictingTeachingBuildingNumber(keyword, item.getSnippet())) {
                continue;
            }
            String haystack = title + "|" + snippet + "|" + normalizeSearchText(item.getTypeDes());
            int score = Integer.MAX_VALUE;
            if (title.equals(normalizedKey) || title.equals(normalizeSearchText("西南大学" + keyword))) {
                score = 0;
            } else if (!TextUtils.isEmpty(title)
                    && (title.contains(normalizedKey) || normalizedKey.contains(title))) {
                score = 1;
            } else if (haystack.contains(normalizedKey)) {
                score = 2;
            } else if (title.contains("西南大学")) {
                score = 3;
            }
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            double distance = calculateDistanceMeters(
                    CampusMapManager.CAMPUS_CENTER_LAT,
                    CampusMapManager.CAMPUS_CENTER_LNG,
                    lat,
                    lng);
            if (score < bestScore || (score == bestScore && distance < bestDistance)) {
                best = item;
                bestScore = score;
                bestDistance = distance;
            }
        }
        return best;
    }

    private LocationDto mergeAmapPoiWithContent(@NonNull String keyword, @NonNull PoiItem poi) {
        LocationDto content = findBestContentForAmapPoi(keyword, poi);
        if (content != null) {
            return content;
        }
        LocationDto dto = new LocationDto();
        dto.id = allocateAmapSearchLocationId();
        dto.name = safeText(poi.getTitle(), keyword);
        dto.category = safeText(poi.getTypeDes(), "高德地点");
        dto.address = safeText(poi.getSnippet(), "西南大学");
        if (poi.getLatLonPoint() != null) {
            dto.latitude = poi.getLatLonPoint().getLatitude();
            dto.longitude = poi.getLatLonPoint().getLongitude();
        }
        dto.radiusMeters = DEFAULT_RADIUS_METERS;
        dto.description = dto.name + "。该地点坐标来自高德POI，暂无数据库讲解内容。";
        return dto;
    }

    private LocationDto mergeAmapGeocodeWithContent(@NonNull String keyword,
                                                    @NonNull GeocodeAddress address) {
        LocationDto content = findBestContentByKeywordV2(keyword);
        if (content != null) {
            return content;
        }
        LocationDto dto = new LocationDto();
        dto.id = allocateAmapSearchLocationId();
        String buildingNumber = extractTeachingBuildingNumber(keyword);
        if (!TextUtils.isEmpty(buildingNumber)) {
            dto.name = buildingNumber + "号教学楼";
        } else {
            dto.name = safeText(keyword, safeText(address.getFormatAddress(), "高德地点"));
        }
        dto.category = "高德地点";
        dto.address = safeText(address.getFormatAddress(), "西南大学");
        if (address.getLatLonPoint() != null) {
            dto.latitude = address.getLatLonPoint().getLatitude();
            dto.longitude = address.getLatLonPoint().getLongitude();
        }
        dto.radiusMeters = DEFAULT_RADIUS_METERS;
        dto.description = dto.name + "。该地点坐标来自高德搜索，暂无数据库讲解内容。";
        return dto;
    }

    @Nullable
    private LocationDto findBestContentForAmapPoi(@NonNull String keyword, @NonNull PoiItem poi) {
        Set<String> keywordAliases = buildNormalizedSearchAliases(keyword);
        keywordAliases.addAll(buildNormalizedSearchAliases(safeText(poi.getTitle(), "")));
        String title = normalizeSearchText(poi.getTitle());
        String snippet = normalizeSearchText(poi.getSnippet());
        LocationDto best = null;
        int bestScore = Integer.MAX_VALUE;
        for (LocationDto location : locations) {
            if (location == null || location.id >= AMAP_SEARCH_LOCATION_ID_BASE) {
                continue;
            }
            String name = normalizeSearchText(location.name);
            String category = normalizeSearchText(location.category);
            String address = normalizeSearchText(location.address);
            if (hasConflictingTeachingBuildingNumber(keyword, location.name)
                    || hasConflictingTeachingBuildingNumber(keyword, location.address)) {
                continue;
            }
            int score = Integer.MAX_VALUE;
            if (scoreNormalizedTextAgainstAliases(name, keywordAliases) == 0
                    || (!TextUtils.isEmpty(title)
                    && (name.equals(title) || title.contains(name) || name.contains(title)))) {
                score = 0;
            } else if (scoreNormalizedTextAgainstAliases(name, keywordAliases) == 1) {
                score = 1;
            } else if (address.contains(title) || snippet.contains(name)) {
                score = 2;
            } else if (containsAnySearchAlias(category, keywordAliases)
                    || containsAnySearchAlias(address, keywordAliases)) {
                score = 3;
            }
            if (score < bestScore) {
                best = location;
                bestScore = score;
            }
        }
        return best;
    }

    private void upsertSearchLocation(@NonNull LocationDto location) {
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).id == location.id) {
                locations.set(i, location);
                return;
            }
        }
        locations.add(location);
    }

    private long allocateAmapSearchLocationId() {
        while (containsLocationId(nextAmapSearchLocationId)) {
            nextAmapSearchLocationId++;
        }
        return nextAmapSearchLocationId++;
    }

    private boolean containsLocationId(long id) {
        for (LocationDto location : locations) {
            if (location != null && location.id == id) {
                return true;
            }
        }
        return false;
    }

    private void focusLocation(@Nullable LocationDto location) {
        if (location == null || !isMapReady() || markerManager == null) {
            return;
        }
        GeoPoint markerPos = markerManager.getMarkerPosition(location.id);
        if (markerPos != null) {
            markerManager.setSelectedLocationId(location.id);
            markerManager.focusMarker(location.id);
            mapCameraMode = MapCameraMode.LOCKED_ON_SELECTION;
            lockedSelectionPoint = markerPos;
            lockedRouteBounds = null;
            followMyLocation = false;
            keepMapOnUserResult = true;
            hasAutoCentered = true;
            campusMapManager.setCenterAndZoom(markerPos, SELECTION_CAMERA_ZOOM);
            return;
        }
        GeoPoint display = resolveDisplayPoint(location);
        if (display != null) {
            lockMapCameraOnSelection(display);
            markerManager.setSelectedLocationId(location.id);
            Marker marker = markerManager.getMarker(location.id);
            if (marker != null) {
                marker.showInfoWindow();
            }
        }
    }

    /** 搜索/附近景点：移动地图并弹出 Marker 气泡。 */
    private void applySearchFocusOnLocation(@NonNull LocationDto location) {
        focusLocation(location);
    }

    private void requestAmapPoiCoordinateFix(@Nullable LocationDto location, boolean focusAfterUpdate) {
        if (location == null || TextUtils.isEmpty(location.name)) {
            return;
        }
        if (hasPlottableLocation(location)) {
            return;
        }
        if (pendingPoiCoordinateFixes.contains(location.id)) {
            return;
        }
        pendingPoiCoordinateFixes.add(location.id);
        PoiSearch.Query query = new PoiSearch.Query(buildAmapCampusQuery(location.name), "", "重庆");
        query.setPageSize(10);
        query.setPageNum(0);
        query.setCityLimit(false);
        query.setDistanceSort(true);
        query.setLocation(new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG));
        query.setExtensions(PoiSearch.EXTENSIONS_BASE);
        try {
            PoiSearch search = new PoiSearch(this, query);
            search.setBound(new PoiSearch.SearchBound(
                    new LatLonPoint(CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG),
                    4500,
                    true));
            search.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int code) {
                    runOnUiThreadIfAlive(() -> {
                        pendingPoiCoordinateFixes.remove(location.id);
                        if (code != AMapException.CODE_AMAP_SUCCESS || result == null || result.getPois() == null) {
                            return;
                        }
                        if (hasPlottableLocation(location)) {
                            return;
                        }
                        PoiItem item = findBestAmapPoi(location, result.getPois());
                        if (item == null || item.getLatLonPoint() == null) {
                            return;
                        }
                        GeoPoint corrected = new GeoPoint(
                                item.getLatLonPoint().getLatitude(),
                                item.getLatLonPoint().getLongitude());
                        location.latitude = corrected.getLatitude();
                        location.longitude = corrected.getLongitude();
                        locationDisplayPoints.put(location.id, corrected);
                        poiRouteNodeHints.put(location.id, findNearestRouteNode(corrected));
                        mergeBackendLocationsIntoRouteGraph();
                        renderLocationMarkersAndFences();
                        if (selectedLocation != null && selectedLocation.id == location.id) {
                            Double distance = currentRawGeoPoint == null ? null
                                    : distanceRawToLocationMeters(currentRawGeoPoint, location);
                            selectLocation(location, distance, false, false, false);
                            if (focusAfterUpdate) {
                                focusLocation(location);
                            }
                        }
                    });
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int code) {
                    pendingPoiCoordinateFixes.remove(location.id);
                }
            });
            search.searchPOIAsyn();
        } catch (AMapException e) {
            pendingPoiCoordinateFixes.remove(location.id);
        }
    }

    @Nullable
    private PoiItem findBestAmapPoi(@NonNull LocationDto location, @NonNull List<PoiItem> pois) {
        String target = normalizeSearchText(location.name);
        String campusQuery = normalizeSearchText(buildAmapCampusQuery(location.name));
        String addressHint = normalizeSearchText(location.address);
        PoiItem best = null;
        int bestScore = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (PoiItem item : pois) {
            if (item == null || item.getLatLonPoint() == null) {
                continue;
            }
            double lat = item.getLatLonPoint().getLatitude();
            double lng = item.getLatLonPoint().getLongitude();
            if (!isCampusPoint(lat, lng)) {
                continue;
            }
            String title = normalizeSearchText(item.getTitle());
            String snippet = normalizeSearchText(item.getSnippet());
            if (hasConflictingTeachingBuildingNumber(location.name, item.getTitle())
                    || hasConflictingTeachingBuildingNumber(location.name, item.getSnippet())) {
                continue;
            }
            int score = Integer.MAX_VALUE;
            if (title.equals(target) || title.equals(campusQuery)) {
                score = 0;
            } else if (!TextUtils.isEmpty(title)
                    && (title.contains(target) || target.contains(title)
                    || title.contains(campusQuery) || campusQuery.contains(title))) {
                score = 1;
            } else if (!TextUtils.isEmpty(addressHint)
                    && (title.contains(addressHint) || snippet.contains(addressHint))) {
                score = 2;
            } else if (snippet.contains(target) || snippet.contains(campusQuery)) {
                score = 3;
            }
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            double distance = calculateDistanceMeters(
                    CampusMapManager.CAMPUS_CENTER_LAT,
                    CampusMapManager.CAMPUS_CENTER_LNG,
                    lat,
                    lng);
            if (score < bestScore || (score == bestScore && distance < bestDistance)) {
                best = item;
                bestScore = score;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void manualPlaySelected() {
        if (selectedLocation == null) {
            toast(getString(R.string.toast_select_location_first));
            return;
        }
        toggleGuideForLocation(selectedLocation);
    }

    /** 底栏「路线」/导航：仅按起点、终点下拉规划，与讲解点无关。 */
    private void planRouteFromSpinners() {
        if (resolveRouteDestination() == null) {
            toast(getString(R.string.toast_select_destination_first));
            return;
        }
        showRouteToSelected();
    }

    private void showRouteToSelected() {
        LocationDto destination = resolveRouteDestination();
        if (destination == null) {
            toast(getString(R.string.toast_select_destination_first));
            return;
        }
        GeoPoint target = resolveRouteEndPoint(destination);
        if (target == null) {
            toast(getString(R.string.toast_place_not_found));
            return;
        }
        holdUserMapResult();
        beginRoutePlanning();
        GeoPoint routeStart = resolveCampusRouteStartPoint();
        lastRouteStartPoint = routeStart;
        routedLocationId = destination.id;
        pendingRouteLocationId = destination.id;
        pendingRouteMode = routeMode;
        if (cardRoutePanel != null) {
            cardRoutePanel.setVisibility(View.VISIBLE);
        }
        if (tvRouteSummary != null) {
            tvRouteSummary.setText("正在调用高德路线规划...");
        }
        if (tvRouteSteps != null) {
            tvRouteSteps.setVisibility(View.GONE);
        }

        if (routeSearch == null) {
            drawFallbackCampusRoute(routeStart, target, destination, "高德路线服务初始化失败，已使用直线兜底路线");
            return;
        }

        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                toLatLonPoint(routeStart),
                toLatLonPoint(target));
        try {
            if (routeMode == RouteMode.BUS) {
                RouteSearch.BusRouteQuery query = new RouteSearch.BusRouteQuery(
                        fromAndTo,
                        RouteSearch.BusDefault,
                        "重庆",
                        0);
                query.setExtensions(RouteSearch.EXTENSIONS_ALL);
                routeSearch.calculateBusRouteAsyn(query);
            } else if (routeMode == RouteMode.DRIVE || routeMode == RouteMode.TAXI) {
                RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(
                        fromAndTo,
                        RouteSearch.DrivingShortDistance,
                        null,
                        null,
                        "");
                query.setExtensions(RouteSearch.EXTENSIONS_ALL);
                routeSearch.calculateDriveRouteAsyn(query);
            } else {
                RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(
                        fromAndTo,
                        RouteSearch.WalkMultipath);
                query.setExtensions(RouteSearch.EXTENSIONS_ALL);
                routeSearch.calculateWalkRouteAsyn(query);
            }
        } catch (Exception e) {
            drawFallbackCampusRoute(routeStart, target, destination, "高德路线规划请求失败，已使用直线兜底路线");
        }
    }

    /** 校内路网 Dijkstra 最短路径（兜底）。 */
    private void drawCampusGraphRoute(@NonNull LocationDto destination) {
        if (!isMapReady()) {
            return;
        }
        GeoPoint target = resolveRouteEndPoint(destination);
        if (target == null) {
            toast(getString(R.string.toast_place_not_found));
            return;
        }
        holdUserMapResult();
        clearRoutePolylineOnly();
        GeoPoint routeStart = resolveCampusRouteStartPoint();
        List<GeoPoint> routePoints = buildCampusRoute(routeStart, target, destination);
        renderActiveRoutePolyline(routePoints);
        routedLocationId = destination.id;
        pendingRouteLocationId = -1L;
        pendingRouteMode = null;
        double distance = calculatePathDistanceMeters(routePoints);
        boolean outsideCampus = currentRawGeoPoint != null && !isInCampusSmart(currentRawGeoPoint);
        String stepText;
        if (distance > 50000) {
            stepText = getString(R.string.label_route_far_tip);
            setRouteDetailUi(getString(R.string.label_far_from_campus), stepText);
        } else {
            String routeHint = describeRouteNodes(routePoints, destination);
            stepText = buildRouteStepText(routeStart, target, destination, routeHint);
            if (outsideCampus) {
                stepText = "当前在校园外，已从西南大学南门开始规划。 " + stepText;
            }
            int minutes = estimateRouteMinutes(distance);
            String modeName = getRouteModeLabel(routeMode);
            String summary = getString(R.string.label_estimate_time_mode, modeName, minutes, distance / 1000.0);
            setRouteDetailUi(summary, stepText);
        }
        presentActiveRouteView(routePoints, destination, stepText);
        setBottomPanelCollapsed(false, true);
    }

    private void setRouteDetailUi(@NonNull String summary, @NonNull String steps) {
        if (cardRoutePanel != null) {
            cardRoutePanel.setVisibility(View.VISIBLE);
        }
        if (tvRouteSummary != null) {
            tvRouteSummary.setText(summary);
        }
        if (tvRouteSteps != null) {
            tvRouteSteps.setVisibility(View.GONE);
        }
    }

    private void presentActiveRouteView(@NonNull List<GeoPoint> routePoints,
                                        @NonNull LocationDto destination,
                                        @NonNull String roadText) {
        List<GeoPoint> panelReference = buildRoutePanelReference(routePoints, destination);
        activeRouteWaypointIds.clear();
        activeRouteWaypointIds.addAll(collectRouteWaypointIds(panelReference, destination));
        updateRouteWaypointPanel(panelReference, destination);
    }

    private void updateRouteWaypointPanel(@NonNull List<GeoPoint> routePoints,
                                          @NonNull LocationDto destination) {
        RoutePanelSections sections = buildRoutePanelSections(routePoints, destination);
        if (routeDirectionsAdapter != null && rvRouteDirections != null) {
            routeDirectionsAdapter.submit(sections.directions);
            rvRouteDirections.post(() -> RecyclerViewHeightUtil.expandToFitChildren(rvRouteDirections));
        }
        if (routeNamedPlacesAdapter != null && rvRouteNamedPlaces != null) {
            routeNamedPlacesAdapter.submit(sections.namedPlaces);
            rvRouteNamedPlaces.post(() -> RecyclerViewHeightUtil.expandToFitChildren(rvRouteNamedPlaces));
        }
    }

    @NonNull
    private RoutePanelSections buildRoutePanelSections(@NonNull List<GeoPoint> routePoints,
                                                       @NonNull LocationDto destination) {
        List<RouteWaypointAdapter.Item> directions = buildRouteDirections();
        List<RouteWaypointAdapter.Item> namedPlaces = buildRouteNamedPlaces(routePoints, destination);
        return new RoutePanelSections(directions, namedPlaces);
    }

    @NonNull
    private List<RouteWaypointAdapter.Item> buildRouteDirections() {
        List<RouteWaypointAdapter.Item> directions = new ArrayList<>();
        String lastLabel = null;
        if (!lastAmapRouteSteps.isEmpty()) {
            for (AmapRouteStep step : lastAmapRouteSteps) {
                if (directions.size() >= ROUTE_WAYPOINT_MAX_COUNT) {
                    break;
                }
                String label = sanitizeRouteInstruction(step.instruction);
                if (TextUtils.isEmpty(label) || label.equals(lastLabel)) {
                    continue;
                }
                directions.add(new RouteWaypointAdapter.Item(label, -1L, false));
                lastLabel = label;
            }
            return directions;
        }
        directions.add(new RouteWaypointAdapter.Item("沿规划路线前行", -1L, false));
        return directions;
    }

    @NonNull
    private List<RouteWaypointAdapter.Item> buildRouteNamedPlaces(@NonNull List<GeoPoint> routePoints,
                                                                 @NonNull LocationDto destination) {
        List<RouteWaypointAdapter.Item> namedPlaces = new ArrayList<>();
        Set<Long> addedSpotIds = new HashSet<>();
        Set<String> addedLabels = new HashSet<>();

        long startSpotId = parseRouteEndLocId(selectedRouteStartId);
        LocationDto startSpot = startSpotId > 0 ? findLocationById(startSpotId) : null;
        if (isDatabaseCampusSpot(startSpot)) {
            addDatabaseWaypoint(namedPlaces, addedSpotIds, startSpot, false);
            addedLabels.add(normalizeSearchText(startSpot.name));
        } else {
            String startLabel = resolveRouteStartLabel();
            namedPlaces.add(new RouteWaypointAdapter.Item(startLabel, -1L, false));
            addedLabels.add(normalizeSearchText(startLabel));
        }

        appendDatabaseSpotsAlongRoute(namedPlaces, addedSpotIds, addedLabels, routePoints, destination.id, startSpotId);
        appendRoadNamesFromSteps(namedPlaces, addedLabels);

        if (!addedSpotIds.contains(destination.id)) {
            addDatabaseWaypoint(namedPlaces, addedSpotIds, destination, true);
        }
        return namedPlaces;
    }

    private void appendDatabaseSpotsAlongRoute(@NonNull List<RouteWaypointAdapter.Item> items,
                                               @NonNull Set<Long> addedSpotIds,
                                               @NonNull Set<String> addedLabels,
                                               @NonNull List<GeoPoint> routePoints,
                                               long destinationId,
                                               long startSpotId) {
        if (routePoints.size() < 2) {
            return;
        }
        List<LocationDto> alongRoute = new ArrayList<>();
        for (LocationDto location : locations) {
            if (!isDatabaseCampusSpot(location)) {
                continue;
            }
            if (location.id == destinationId || location.id == startSpotId || addedSpotIds.contains(location.id)) {
                continue;
            }
            GeoPoint point = resolveDisplayPoint(location);
            if (point == null) {
                continue;
            }
            RouteNearestPoint nearest = findNearestRoutePoint(point, routePoints);
            if (nearest.distanceMeters <= ROUTE_LANDMARK_RADIUS_METERS * 1.2) {
                alongRoute.add(location);
            }
        }
        alongRoute.sort(Comparator.comparingDouble(loc -> {
            GeoPoint p = resolveDisplayPoint(loc);
            return p == null ? Double.MAX_VALUE : distanceAlongPolyline(routePoints, p);
        }));
        for (LocationDto spot : alongRoute) {
            if (items.size() >= ROUTE_WAYPOINT_MAX_COUNT) {
                break;
            }
            addDatabaseWaypoint(items, addedSpotIds, spot, false);
            if (spot.name != null) {
                addedLabels.add(normalizeSearchText(spot.name));
            }
        }
    }

    private void appendRoadNamesFromSteps(@NonNull List<RouteWaypointAdapter.Item> items,
                                          @NonNull Set<String> addedLabels) {
        for (AmapRouteStep step : lastAmapRouteSteps) {
            if (items.size() >= ROUTE_WAYPOINT_MAX_COUNT) {
                break;
            }
            String road = step.roadName == null ? "" : step.roadName.trim();
            if (TextUtils.isEmpty(road)) {
                continue;
            }
            String normalized = normalizeSearchText(road);
            if (addedLabels.contains(normalized) || containsWaypointLabel(items, road)) {
                continue;
            }
            items.add(new RouteWaypointAdapter.Item(road, -1L, false));
            addedLabels.add(normalized);
        }
    }

    @Nullable
    private LocationDto findDatabaseSpotAtRouteStep(@Nullable GeoPoint stepPoint,
                                                      @NonNull Set<Long> addedSpotIds,
                                                      long destinationId,
                                                      long startSpotId) {
        if (stepPoint == null) {
            return null;
        }
        LocationDto best = null;
        double bestDistance = ROUTE_STEP_SPOT_MATCH_METERS;
        for (LocationDto location : locations) {
            if (!isDatabaseCampusSpot(location) || addedSpotIds.contains(location.id)) {
                continue;
            }
            if (location.id == destinationId || location.id == startSpotId) {
                continue;
            }
            GeoPoint spotPoint = resolveDisplayPoint(location);
            if (spotPoint == null) {
                continue;
            }
            double distance = calculateDistanceMeters(
                    stepPoint.getLatitude(), stepPoint.getLongitude(),
                    spotPoint.getLatitude(), spotPoint.getLongitude());
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = location;
            }
        }
        return best;
    }

    @NonNull
    private String sanitizeRouteInstruction(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replaceAll("<[^>]+>", "").trim();
        if (text.length() > 48) {
            return text.substring(0, 48) + "…";
        }
        return text;
    }

    @NonNull
    private String resolveRouteStartLabel() {
        long spotId = parseRouteEndLocId(selectedRouteStartId);
        if (spotId > 0) {
            LocationDto startSpot = findLocationById(spotId);
            if (startSpot != null && !TextUtils.isEmpty(startSpot.name)) {
                return startSpot.name;
            }
        }
        return getString(R.string.label_route_start);
    }

    private boolean isDatabaseCampusSpot(@Nullable LocationDto location) {
        return location != null && location.id > 0 && location.id < AMAP_SEARCH_LOCATION_ID_BASE;
    }

    private boolean isSearchPointOnCampus(@NonNull GeoPoint point) {
        double distanceFromCenter = calculateDistanceMeters(
                point.getLatitude(), point.getLongitude(),
                CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG);
        return isInCampus(point.getLatitude(), point.getLongitude())
                || distanceFromCenter <= AMAP_POI_ACCEPT_RADIUS_METERS;
    }

    private void presentCampusSpotSearchResult(@NonNull LocationDto location) {
        GeoPoint point = resolveDisplayPoint(location);
        if (point != null) {
            locationDisplayPoints.put(location.id, point);
            poiRouteNodeHints.put(location.id, findNearestRouteNode(point));
        }
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, location);
        clearRouteIfDestinationChanged(location.id);
        selectLocation(location, distance, false, false, false);
        applySearchFocusOnLocation(location);
        if (tvRouteSummary != null) {
            tvRouteSummary.setText("已定位景点：" + location.name);
        }
        if (tvRouteSteps != null) {
            tvRouteSteps.setText("");
        }
    }

    private void presentNonCampusSearchResult(@NonNull LocationDto location, @NonNull GeoPoint point) {
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, location);
        clearRouteIfDestinationChanged(location.id);
        selectLocation(location, distance, true, false, false);
        if (markerManager != null && markerManager.getMarker(location.id) == null) {
            renderLocationMarkersAndFences();
        }
        applySearchFocusOnLocation(location);
        if (tvRouteSummary != null) {
            tvRouteSummary.setText("已定位：" + location.name);
        }
        if (tvRouteSteps != null) {
            tvRouteSteps.setText("");
        }
    }

    private static final class ContentMatch {
        final LocationDto location;
        final int score;

        ContentMatch(@NonNull LocationDto location, int score) {
            this.location = location;
            this.score = score;
        }
    }

    @Nullable
    private ContentMatch findBestContentMatch(@NonNull String keyword) {
        Set<String> aliases = buildNormalizedSearchAliases(keyword);
        LocationDto best = null;
        int bestScore = Integer.MAX_VALUE;
        for (LocationDto location : locations) {
            if (location == null || location.id >= AMAP_SEARCH_LOCATION_ID_BASE) {
                continue;
            }
            String name = normalizeSearchText(location.name);
            String category = normalizeSearchText(location.category);
            String address = normalizeSearchText(location.address);
            if (hasConflictingTeachingBuildingNumber(keyword, location.name)
                    || hasConflictingTeachingBuildingNumber(keyword, location.address)) {
                continue;
            }
            int score = Integer.MAX_VALUE;
            int nameScore = scoreNormalizedTextAgainstAliases(name, aliases);
            if (nameScore == 0) {
                score = 0;
            } else if (nameScore == 1) {
                score = 1;
            } else if (containsAnySearchAlias(address, aliases)) {
                score = 2;
            } else if (containsAnySearchAlias(category, aliases)) {
                score = 3;
            }
            if (score < bestScore) {
                best = location;
                bestScore = score;
            }
        }
        return best == null ? null : new ContentMatch(best, bestScore);
    }

    private void addDatabaseWaypoint(@NonNull List<RouteWaypointAdapter.Item> items,
                                     @NonNull Set<Long> addedSpotIds,
                                     @Nullable LocationDto location,
                                     boolean isDestination) {
        if (location == null || addedSpotIds.contains(location.id)) {
            return;
        }
        addedSpotIds.add(location.id);
        String label = isDestination ? location.name + "（终点）" : location.name;
        items.add(new RouteWaypointAdapter.Item(label, location.id, true));
    }

    private boolean containsWaypointLabel(@NonNull List<RouteWaypointAdapter.Item> items, @NonNull String label) {
        String normalized = label.trim();
        for (RouteWaypointAdapter.Item item : items) {
            String existing = item.label.replace("（终点）", "").trim();
            if (normalized.equals(existing) || normalized.equals(item.label.trim())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Long parseRouteLocNodeId(@NonNull String nodeId) {
        if (!nodeId.startsWith("LOC_")) {
            return null;
        }
        try {
            return Long.parseLong(nodeId.substring(4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double distanceAlongPolyline(@NonNull List<GeoPoint> polyline, @NonNull GeoPoint target) {
        RouteNearestPoint nearest = findNearestRoutePoint(target, polyline);
        double along = 0;
        for (int i = 1; i <= nearest.routeIndex && i < polyline.size(); i++) {
            along += DistanceUtil.distanceMeters(polyline.get(i - 1), polyline.get(i));
        }
        return along;
    }

    @NonNull
    private Set<Long> collectRouteWaypointIds(@NonNull List<GeoPoint> routePoints,
                                            @NonNull LocationDto destination) {
        Set<Long> ids = new HashSet<>();
        ids.add(destination.id);
        for (LocationDto location : locations) {
            if (location == null || !hasPlottableLocation(location)) {
                continue;
            }
            GeoPoint point = resolveDisplayPoint(location);
            if (point == null) {
                continue;
            }
            RouteNearestPoint nearest = findNearestRoutePoint(point, routePoints);
            if (nearest.distanceMeters <= ROUTE_LANDMARK_RADIUS_METERS * 1.5) {
                ids.add(location.id);
            }
        }
        return ids;
    }

    @Nullable
    private LocationDto resolveRouteDestination() {
        if (ROUTE_END_DESTINATION.equals(selectedRouteEndId)) {
            return null;
        }
        long locationId = parseRouteEndLocId(selectedRouteEndId);
        if (locationId > 0) {
            LocationDto byEnd = findLocationById(locationId);
            if (byEnd != null) {
                return byEnd;
            }
        }
        return null;
    }

    private boolean tryShowAmapRouteToSelected() {
        if (resolveRouteDestination() == null) {
            toast(getString(R.string.toast_select_destination_first));
            return true;
        }
        showRouteToSelected();
        return true;
    }

    private void handleAmapDriveRouteResult(@Nullable DriveRouteResult result, int code) {
        runOnUiThreadIfAlive(() -> handleAmapDriveRouteResultOnUi(result, code));
    }

    private void handleAmapDriveRouteResultOnUi(@Nullable DriveRouteResult result, int code) {
        if (!isMapReady() || !isCurrentPendingDriveRoute()) {
            return;
        }
        if (code != AMapException.CODE_AMAP_SUCCESS || result == null
                || result.getPaths() == null || result.getPaths().isEmpty()) {
            drawFallbackRouteForPendingSelection("高德驾车路线不可用，已使用校内道路兜底路线");
            return;
        }
        DrivePath path = chooseShortestDrivePath(result.getPaths());
        List<GeoPoint> points = collectDriveRoutePoints(path);
        if (points.size() < 2) {
            drawFallbackRouteForPendingSelection("高德驾车路线无有效折线，已使用校内道路兜底路线");
            return;
        }
        drawAmapRoute(points, path.getDistance(), path.getDuration(), collectDriveRouteSteps(path));
    }

    private void handleAmapBusRouteResult(@Nullable BusRouteResult result, int code) {
        runOnUiThreadIfAlive(() -> handleAmapBusRouteResultOnUi(result, code));
    }

    private void handleAmapBusRouteResultOnUi(@Nullable BusRouteResult result, int code) {
        if (!isMapReady() || !isCurrentPendingRoute(RouteMode.BUS)) {
            return;
        }
        if (code != AMapException.CODE_AMAP_SUCCESS || result == null
                || result.getPaths() == null || result.getPaths().isEmpty()) {
            drawFallbackRouteForPendingSelection("高德公交路线不可用，已使用校内道路兜底路线");
            return;
        }
        BusPath path = chooseShortestBusPath(result.getPaths());
        List<GeoPoint> points = collectBusRoutePoints(path);
        if (points.size() < 2) {
            drawFallbackRouteForPendingSelection("高德公交路线无有效折线，已使用校内道路兜底路线");
            return;
        }
        drawAmapRoute(points, path.getDistance(), path.getDuration(), collectBusRouteSteps(path));
    }

    private void handleAmapWalkRouteResult(@Nullable WalkRouteResult result, int code) {
        runOnUiThreadIfAlive(() -> handleAmapWalkRouteResultOnUi(result, code));
    }

    private void handleAmapWalkRouteResultOnUi(@Nullable WalkRouteResult result, int code) {
        if (!isMapReady() || !isCurrentPendingRoute(RouteMode.WALK)) {
            return;
        }
        if (code != AMapException.CODE_AMAP_SUCCESS || result == null
                || result.getPaths() == null || result.getPaths().isEmpty()) {
            drawFallbackRouteForPendingSelection("高德步行路线不可用，已使用校内道路兜底路线");
            return;
        }
        WalkPath path = chooseShortestWalkPath(result.getPaths());
        List<GeoPoint> points = collectWalkRoutePoints(path);
        if (points.size() < 2) {
            drawFallbackRouteForPendingSelection("高德步行路线无有效折线，已使用校内道路兜底路线");
            return;
        }
        drawAmapRoute(points, path.getDistance(), path.getDuration(), collectWalkRouteSteps(path));
    }

    private boolean isCurrentPendingDriveRoute() {
        if (pendingRouteLocationId < 0 || pendingRouteMode == null) {
            return false;
        }
        LocationDto destination = resolveRouteDestination();
        return destination != null
                && pendingRouteLocationId == destination.id
                && (pendingRouteMode == RouteMode.DRIVE || pendingRouteMode == RouteMode.TAXI);
    }

    private boolean isCurrentPendingRoute(@NonNull RouteMode callbackMode) {
        if (pendingRouteLocationId < 0 || pendingRouteMode == null) {
            return false;
        }
        LocationDto destination = resolveRouteDestination();
        return destination != null
                && pendingRouteLocationId == destination.id
                && pendingRouteMode == callbackMode;
    }

    @Nullable
    private DrivePath chooseShortestDrivePath(@Nullable List<DrivePath> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        DrivePath best = null;
        double bestDistance = Double.MAX_VALUE;
        for (DrivePath path : paths) {
            if (path == null) {
                continue;
            }
            double distance = path.getDistance() > 0
                    ? path.getDistance()
                    : calculatePathDistanceMeters(collectDriveRoutePoints(path));
            if (distance < bestDistance) {
                best = path;
                bestDistance = distance;
            }
        }
        return best == null ? paths.get(0) : best;
    }

    @Nullable
    private BusPath chooseShortestBusPath(@Nullable List<BusPath> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        BusPath best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BusPath path : paths) {
            if (path == null) {
                continue;
            }
            double distance = path.getDistance() > 0
                    ? path.getDistance()
                    : calculatePathDistanceMeters(collectBusRoutePoints(path));
            if (distance < bestDistance) {
                best = path;
                bestDistance = distance;
            }
        }
        return best == null ? paths.get(0) : best;
    }

    @Nullable
    private WalkPath chooseShortestWalkPath(@Nullable List<WalkPath> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        WalkPath best = null;
        double bestDistance = Double.MAX_VALUE;
        for (WalkPath path : paths) {
            if (path == null) {
                continue;
            }
            double distance = path.getDistance() > 0
                    ? path.getDistance()
                    : calculatePathDistanceMeters(collectWalkRoutePoints(path));
            if (distance < bestDistance) {
                best = path;
                bestDistance = distance;
            }
        }
        return best == null ? paths.get(0) : best;
    }

    private void drawAmapRoute(@NonNull List<GeoPoint> points,
                               double distanceMeters,
                               long durationSeconds,
                               @NonNull List<AmapRouteStep> steps) {
        if (!isMapReady()) {
            return;
        }
        lastAmapRouteSteps.clear();
        lastAmapRouteSteps.addAll(steps);
        LocationDto destination = resolveRouteDestination();
        GeoPoint routeStart = lastRouteStartPoint;
        if (routeStart == null && !points.isEmpty()) {
            routeStart = points.get(0);
            lastRouteStartPoint = routeStart;
        }
        GeoPoint routeEnd = destination == null ? null : resolveRouteEndPoint(destination);
        List<GeoPoint> mapDrawPoints = chooseMapRouteDrawPoints(points, destination, distanceMeters);
        if (mapDrawPoints.size() < 2 && routeStart != null && routeEnd != null) {
            mapDrawPoints = prepareMapRouteDrawPoints(
                    buildCampusRoute(routeStart, routeEnd, destination), routeStart, routeEnd);
        }
        drawRoutePathPointsResolved(mapDrawPoints);
        if (destination != null) {
            routedLocationId = destination.id;
        }
        pendingRouteLocationId = -1L;
        pendingRouteMode = null;

        double displayDistance = distanceMeters > 0 ? distanceMeters : calculatePathDistanceMeters(mapDrawPoints);
        int minutes = durationSeconds > 0
                ? Math.max(1, (int) Math.ceil(durationSeconds / 60.0))
                : estimateRouteMinutes(displayDistance);
        String modeName = getRouteModeLabel(routeMode);
        String summary = getString(R.string.label_estimate_time_mode, modeName, minutes, displayDistance / 1000.0);
        List<String> instructionLabels = new ArrayList<>();
        for (AmapRouteStep step : steps) {
            String label = sanitizeRouteInstruction(step.instruction);
            if (!TextUtils.isEmpty(label)) {
                instructionLabels.add(label);
            }
        }
        List<String> compact = compactRouteInstructions(instructionLabels, 6);
        String roadText = compact.isEmpty()
                ? "经过道路：已按高德真实道路生成"
                : "经过道路：" + TextUtils.join(" -> ", compact);
        setRouteDetailUi(summary, roadText);
        if (destination != null) {
            presentActiveRouteView(points, destination, roadText);
        } else {
            focusMapOnRoute(mapDrawPoints);
        }
        setBottomPanelCollapsed(false, true);
    }

    private void drawFallbackRouteForPendingSelection(@NonNull String message) {
        LocationDto destination = resolveRouteDestination();
        if (destination == null) {
            return;
        }
        GeoPoint target = resolveDisplayPoint(destination);
        if (target == null) {
            return;
        }
        GeoPoint routeStart = lastRouteStartPoint != null
                ? lastRouteStartPoint
                : resolveCampusRouteStartPoint();
        drawFallbackCampusRoute(routeStart, target, destination, message);
    }

    private void drawFallbackCampusRoute(@NonNull GeoPoint routeStart,
                                         @NonNull GeoPoint target,
                                         @Nullable LocationDto destination,
                                         @NonNull String message) {
        if (!isMapReady()) {
            return;
        }
        List<GeoPoint> routePoints = buildCampusRoute(routeStart, target, destination);
        List<AmapRouteStep> fallbackSteps = buildPolylineFallbackSteps(routePoints);
        lastAmapRouteSteps.clear();
        lastAmapRouteSteps.addAll(fallbackSteps);
        List<GeoPoint> mapDrawPoints = prepareMapRouteDrawPoints(routePoints, routeStart, target);
        drawRoutePathPointsResolved(mapDrawPoints);
        if (destination != null) {
            routedLocationId = destination.id;
        }
        pendingRouteLocationId = -1L;
        pendingRouteMode = null;

        double distance = calculatePathDistanceMeters(routePoints);
        int minutes = estimateRouteMinutes(distance);
        String routeHint = describeRouteNodes(routePoints, destination);
        String modeName = getRouteModeLabel(routeMode);
        String summary = getString(R.string.label_estimate_time_mode, modeName, minutes, distance / 1000.0);
        String nodeText = buildRouteStepText(routeStart, target, destination, routeHint);
        String roadText = "经过道路：" + message + "：" + nodeText;
        setRouteDetailUi(summary, roadText);
        if (destination != null) {
            presentActiveRouteView(routePoints, destination, roadText);
        } else {
            focusMapOnRoute(mapDrawPoints);
        }
        setBottomPanelCollapsed(false, true);
    }

    @NonNull
    private List<GeoPoint> collectRoutePolylinePoints(@Nullable List<LatLonPoint> pathPolyline,
                                                      @Nullable List<? extends List<LatLonPoint>> stepPolylines) {
        List<GeoPoint> mergedSteps = new ArrayList<>();
        if (stepPolylines != null) {
            for (List<LatLonPoint> stepPolyline : stepPolylines) {
                appendLatLonPointsRaw(mergedSteps, stepPolyline);
            }
        }
        List<GeoPoint> stepDeduped = dedupeExactRoutePoints(mergedSteps);
        List<GeoPoint> pathDeduped = dedupeExactRoutePoints(collectLatLonPolyline(pathPolyline));
        List<GeoPoint> combined = dedupeExactRoutePoints(concatGeoPoints(stepDeduped, pathDeduped));
        return chooseRicherRoutePolyline(combined, chooseRicherRoutePolyline(stepDeduped, pathDeduped));
    }

    @NonNull
    private List<GeoPoint> collectLatLonPolyline(@Nullable List<LatLonPoint> polyline) {
        List<GeoPoint> points = new ArrayList<>();
        appendLatLonPointsRaw(points, polyline);
        return points;
    }

    @NonNull
    private List<GeoPoint> concatGeoPoints(@NonNull List<GeoPoint> first,
                                           @NonNull List<GeoPoint> second) {
        List<GeoPoint> merged = new ArrayList<>(first);
        for (GeoPoint point : second) {
            appendRoutePolylinePoint(merged, point);
        }
        return merged;
    }

    @NonNull
    private List<GeoPoint> chooseRicherRoutePolyline(@NonNull List<GeoPoint> primary,
                                                     @NonNull List<GeoPoint> secondary) {
        double primarySpan = calculatePathDistanceMeters(primary);
        double secondarySpan = calculatePathDistanceMeters(secondary);
        if (primary.size() >= 2 && primarySpan >= secondarySpan) {
            return primary;
        }
        if (secondary.size() >= 2) {
            return secondary;
        }
        return primary.size() >= 2 ? primary : secondary;
    }

    private List<GeoPoint> collectDriveRoutePoints(@Nullable DrivePath path) {
        if (path == null) {
            return new ArrayList<>();
        }
        List<List<LatLonPoint>> stepPolylines = new ArrayList<>();
        if (path.getSteps() != null) {
            for (DriveStep step : path.getSteps()) {
                if (step != null) {
                    stepPolylines.add(step.getPolyline());
                }
            }
        }
        return collectRoutePolylinePoints(path.getPolyline(), stepPolylines);
    }

    private List<String> collectDriveInstructions(@Nullable DrivePath path) {
        List<String> instructions = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return instructions;
        }
        for (DriveStep step : path.getSteps()) {
            if (step != null && !TextUtils.isEmpty(step.getInstruction())) {
                instructions.add(step.getInstruction());
            }
        }
        return instructions;
    }

    private List<GeoPoint> collectWalkRoutePoints(@Nullable WalkPath path) {
        List<GeoPoint> points = new ArrayList<>();
        if (path == null) {
            return points;
        }
        if (path.getSteps() != null) {
            for (WalkStep step : path.getSteps()) {
                if (step != null) {
                    addLatLonPoints(points, step.getPolyline());
                }
            }
        }
        if (points.isEmpty()) {
            addLatLonPoints(points, path.getPolyline());
        }
        return points;
    }

    @NonNull
    private List<GeoPoint> dedupeExactRoutePoints(@NonNull List<GeoPoint> source) {
        List<GeoPoint> deduped = new ArrayList<>();
        for (GeoPoint point : source) {
            if (point == null) {
                continue;
            }
            if (!deduped.isEmpty()) {
                GeoPoint last = deduped.get(deduped.size() - 1);
                if (last.getLatitude() == point.getLatitude()
                        && last.getLongitude() == point.getLongitude()) {
                    continue;
                }
            }
            deduped.add(point);
        }
        return deduped;
    }

    /** 绘制折线时使用：仅去掉完全重复坐标，避免 1m 阈值把整条路线压成一点。 */
    private void appendRoutePolylinePoint(@NonNull List<GeoPoint> target, @Nullable GeoPoint point) {
        if (point == null) {
            return;
        }
        if (!target.isEmpty()) {
            GeoPoint last = target.get(target.size() - 1);
            if (last.getLatitude() == point.getLatitude()
                    && last.getLongitude() == point.getLongitude()) {
                return;
            }
        }
        target.add(point);
    }

    private List<String> collectWalkInstructions(@Nullable WalkPath path) {
        List<String> instructions = new ArrayList<>();
        for (AmapRouteStep step : collectWalkRouteSteps(path)) {
            if (!TextUtils.isEmpty(step.instruction)) {
                instructions.add(step.instruction);
            }
        }
        return instructions;
    }

    @NonNull
    private List<AmapRouteStep> collectWalkRouteSteps(@Nullable WalkPath path) {
        List<AmapRouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        for (WalkStep step : path.getSteps()) {
            if (step == null) {
                continue;
            }
            String instruction = sanitizeRouteInstruction(step.getInstruction());
            GeoPoint point = firstPointOfPolyline(step.getPolyline());
            if (TextUtils.isEmpty(instruction) && point == null) {
                continue;
            }
            if (TextUtils.isEmpty(instruction)) {
                instruction = "沿路径前行";
            }
            String road = step.getRoad();
            if (TextUtils.isEmpty(road)) {
                road = extractRoadNameFromInstruction(instruction);
            }
            steps.add(new AmapRouteStep(instruction, point, road));
        }
        return steps;
    }

    @NonNull
    private List<AmapRouteStep> collectDriveRouteSteps(@Nullable DrivePath path) {
        List<AmapRouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        for (DriveStep step : path.getSteps()) {
            if (step == null) {
                continue;
            }
            String instruction = sanitizeRouteInstruction(step.getInstruction());
            GeoPoint point = firstPointOfPolyline(step.getPolyline());
            if (TextUtils.isEmpty(instruction) && point == null) {
                continue;
            }
            String road = step.getRoad();
            if (TextUtils.isEmpty(instruction)) {
                instruction = TextUtils.isEmpty(road) ? "沿道路前行" : "沿" + road;
            }
            if (TextUtils.isEmpty(road)) {
                road = extractRoadNameFromInstruction(instruction);
            }
            steps.add(new AmapRouteStep(instruction, point, road));
        }
        return steps;
    }

    @NonNull
    private List<AmapRouteStep> collectBusRouteSteps(@Nullable BusPath path) {
        List<AmapRouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        for (BusStep step : path.getSteps()) {
            if (step == null) {
                continue;
            }
            RouteBusWalkItem walk = step.getWalk();
            if (walk != null) {
                steps.addAll(collectWalkRouteSteps(walk));
            }
            List<RouteBusLineItem> lines = step.getBusLines();
            if (lines != null && !lines.isEmpty()) {
                for (RouteBusLineItem line : lines) {
                    if (line == null) {
                        continue;
                    }
                    String name = line.getBusLineName();
                    GeoPoint point = firstPointOfPolyline(line.getPolyline());
                    if (!TextUtils.isEmpty(name)) {
                        steps.add(new AmapRouteStep("乘坐" + name, point, name));
                    }
                }
            } else if (step.getBusLine() != null) {
                String name = step.getBusLine().getBusLineName();
                GeoPoint point = firstPointOfPolyline(step.getBusLine().getPolyline());
                if (!TextUtils.isEmpty(name)) {
                    steps.add(new AmapRouteStep("乘坐" + name, point, name));
                }
            }
        }
        return steps;
    }

    @NonNull
    private List<AmapRouteStep> buildPolylineFallbackSteps(@NonNull List<GeoPoint> routePoints) {
        List<AmapRouteStep> steps = new ArrayList<>();
        for (GeoPoint sample : sampleRoutePoints(routePoints, 10)) {
            steps.add(new AmapRouteStep("沿路线前行", sample, null));
        }
        return steps;
    }

    @Nullable
    private String extractRoadNameFromInstruction(@Nullable String instruction) {
        if (TextUtils.isEmpty(instruction)) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("沿([^向左右转进入过，,、]+?)(?:向|行走|步行|前进|左转|右转|直行)")
                .matcher(instruction);
        if (matcher.find()) {
            String road = matcher.group(1).trim();
            return road.isEmpty() ? null : road;
        }
        return null;
    }

    @Nullable
    private GeoPoint firstPointOfPolyline(@Nullable List<LatLonPoint> polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return null;
        }
        LatLonPoint first = polyline.get(0);
        return first == null ? null : toGeoPoint(first);
    }

    private List<GeoPoint> collectBusRoutePoints(@Nullable BusPath path) {
        List<GeoPoint> points = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return points;
        }
        for (BusStep step : path.getSteps()) {
            if (step == null) {
                continue;
            }
            RouteBusWalkItem walk = step.getWalk();
            if (walk != null) {
                addGeoPoints(points, collectWalkRoutePoints(walk));
            }
            List<RouteBusLineItem> lines = step.getBusLines();
            if (lines != null && !lines.isEmpty()) {
                for (RouteBusLineItem line : lines) {
                    if (line != null) {
                        addLatLonPoints(points, line.getPolyline());
                    }
                }
            } else if (step.getBusLine() != null) {
                addLatLonPoints(points, step.getBusLine().getPolyline());
            }
        }
        return points;
    }

    private List<String> collectBusInstructions(@Nullable BusPath path) {
        List<String> instructions = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return instructions;
        }
        for (BusStep step : path.getSteps()) {
            if (step == null) {
                continue;
            }
            RouteBusWalkItem walk = step.getWalk();
            if (walk != null) {
                instructions.addAll(collectWalkInstructions(walk));
            }
            List<RouteBusLineItem> lines = step.getBusLines();
            if (lines != null && !lines.isEmpty()) {
                for (RouteBusLineItem line : lines) {
                    if (line != null && !TextUtils.isEmpty(line.getBusLineName())) {
                        instructions.add("乘坐" + line.getBusLineName());
                    }
                }
            } else if (step.getBusLine() != null && !TextUtils.isEmpty(step.getBusLine().getBusLineName())) {
                instructions.add("乘坐" + step.getBusLine().getBusLineName());
            }
        }
        return instructions;
    }

    private void addLatLonPoints(@NonNull List<GeoPoint> target, @Nullable List<LatLonPoint> source) {
        if (source == null) {
            return;
        }
        for (LatLonPoint point : source) {
            if (point != null) {
                addRoutePoint(target, new GeoPoint(point.getLatitude(), point.getLongitude()));
            }
        }
    }

    private void appendLatLonPointsRaw(@NonNull List<GeoPoint> target, @Nullable List<LatLonPoint> source) {
        if (source == null) {
            return;
        }
        for (LatLonPoint point : source) {
            if (point != null) {
                target.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
            }
        }
    }

    private void addGeoPoints(@NonNull List<GeoPoint> target, @Nullable List<GeoPoint> source) {
        if (source == null) {
            return;
        }
        for (GeoPoint point : source) {
            addRoutePoint(target, point);
        }
    }

    private void addRoutePoint(@NonNull List<GeoPoint> target, @Nullable GeoPoint point) {
        if (point == null) {
            return;
        }
        if (!target.isEmpty()) {
            GeoPoint last = target.get(target.size() - 1);
            if (calculateDistanceMeters(last.getLatitude(), last.getLongitude(),
                    point.getLatitude(), point.getLongitude()) < 1.0) {
                return;
            }
        }
        target.add(point);
    }

    private LatLonPoint toLatLonPoint(@NonNull GeoPoint point) {
        return new LatLonPoint(point.getLatitude(), point.getLongitude());
    }

    private GeoPoint toGeoPoint(@NonNull LatLonPoint point) {
        return new GeoPoint(point.getLatitude(), point.getLongitude());
    }

    private List<String> compactRouteInstructions(@NonNull List<String> source, int maxCount) {
        List<String> compact = new ArrayList<>();
        for (String raw : source) {
            String text = raw == null ? "" : raw.trim();
            if (text.isEmpty()) {
                continue;
            }
            if (!compact.isEmpty() && text.equals(compact.get(compact.size() - 1))) {
                continue;
            }
            compact.add(text);
            if (compact.size() >= maxCount) {
                break;
            }
        }
        return compact;
    }

    private void searchAmapLandmarksAlongRoute(@NonNull List<GeoPoint> routePoints,
                                               @Nullable LocationDto destination,
                                               @NonNull String roadText,
                                               long token) {
        List<GeoPoint> samples = sampleRoutePoints(routePoints, ROUTE_LANDMARK_SAMPLE_COUNT);
        if (samples.isEmpty()) {
            updateRouteLandmarkText(Collections.emptyList(), routePoints, destination, roadText, token);
            return;
        }
        searchAmapLandmarkSample(routePoints, samples, 0, 0, new LinkedHashMap<>(), destination, roadText, token);
    }

    private void searchAmapLandmarkSample(@NonNull List<GeoPoint> routePoints,
                                           @NonNull List<GeoPoint> samples,
                                           int index,
                                           int keywordIndex,
                                           @NonNull Map<String, RouteLandmark> collected,
                                           @Nullable LocationDto destination,
                                           @NonNull String roadText,
                                           long token) {
        if (token != routeLandmarkSearchToken || routePolyline == null) {
            return;
        }
        if (index >= samples.size()) {
            updateRouteLandmarkText(new ArrayList<>(collected.values()), routePoints, destination, roadText, token);
            return;
        }
        if (keywordIndex >= ROUTE_LANDMARK_KEYWORDS.length) {
            searchAmapLandmarkSample(routePoints, samples, index + 1, 0, collected, destination, roadText, token);
            return;
        }

        GeoPoint center = samples.get(index);
        PoiSearch.Query query = new PoiSearch.Query(ROUTE_LANDMARK_KEYWORDS[keywordIndex], "", "重庆");
        query.setPageSize(10);
        query.setPageNum(0);
        query.setCityLimit(false);
        query.setDistanceSort(true);
        query.setLocation(toLatLonPoint(center));
        query.setExtensions(PoiSearch.EXTENSIONS_BASE);
        try {
            PoiSearch search = new PoiSearch(this, query);
            search.setBound(new PoiSearch.SearchBound(
                    toLatLonPoint(center),
                    (int) ROUTE_LANDMARK_RADIUS_METERS,
                    true));
            search.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int code) {
                    if (token != routeLandmarkSearchToken || routePolyline == null) {
                        return;
                    }
                    if (code == AMapException.CODE_AMAP_SUCCESS && result != null && result.getPois() != null) {
                        addAmapPoisAsRouteLandmarks(collected, result.getPois(), routePoints);
                    }
                    searchAmapLandmarkSample(routePoints, samples, index, keywordIndex + 1,
                            collected, destination, roadText, token);
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int code) {
                }
            });
            search.searchPOIAsyn();
        } catch (AMapException e) {
            searchAmapLandmarkSample(routePoints, samples, index, keywordIndex + 1,
                    collected, destination, roadText, token);
        }
    }

    private void addAmapPoisAsRouteLandmarks(@NonNull Map<String, RouteLandmark> target,
                                             @NonNull List<PoiItem> pois,
                                             @NonNull List<GeoPoint> routePoints) {
        for (PoiItem item : pois) {
            if (item == null || item.getLatLonPoint() == null || TextUtils.isEmpty(item.getTitle())) {
                continue;
            }
            String label = item.getTitle().trim();
            if (!isUsefulAmapRouteLandmark(label, item.getTypeDes())) {
                continue;
            }
            GeoPoint point = toGeoPoint(item.getLatLonPoint());
            RouteNearestPoint nearest = findNearestRoutePoint(point, routePoints);
            if (nearest.distanceMeters > ROUTE_LANDMARK_RADIUS_METERS) {
                continue;
            }
            String key = normalizeSearchText(label);
            RouteLandmark current = target.get(key);
            if (current == null
                    || nearest.routeIndex < current.routeIndex
                    || (nearest.routeIndex == current.routeIndex && nearest.distanceMeters < current.distanceMeters)) {
                target.put(key, new RouteLandmark(label, nearest.routeIndex, nearest.distanceMeters));
            }
        }
    }

    private void updateRouteLandmarkText(@NonNull List<RouteLandmark> landmarks,
                                         @NonNull List<GeoPoint> routePoints,
                                         @Nullable LocationDto destination,
                                         @NonNull String roadText,
                                         long token) {
        runOnUiThreadIfAlive(() -> updateRouteLandmarkTextOnUi(landmarks, routePoints, destination, roadText, token));
    }

    private void updateRouteLandmarkTextOnUi(@NonNull List<RouteLandmark> landmarks,
                                         @NonNull List<GeoPoint> routePoints,
                                         @Nullable LocationDto destination,
                                         @NonNull String roadText,
                                         long token) {
        if (token != routeLandmarkSearchToken || routePolyline == null) {
            return;
        }
        if (destination != null && !TextUtils.isEmpty(destination.name)) {
            GeoPoint point = resolveDisplayPoint(destination);
            if (point != null) {
                RouteNearestPoint nearest = findNearestRoutePoint(point, routePoints);
                landmarks.add(new RouteLandmark(destination.name, nearest.routeIndex, nearest.distanceMeters));
            }
        }
        landmarks.sort(Comparator
                .comparingInt((RouteLandmark it) -> it.routeIndex)
                .thenComparingDouble(it -> it.distanceMeters));

        List<String> labels = new ArrayList<>();
        for (RouteLandmark landmark : landmarks) {
            if (landmark == null || TextUtils.isEmpty(landmark.label)) {
                continue;
            }
            String label = landmark.label.trim();
            if (containsNormalizedLabel(labels, label)) {
                continue;
            }
            labels.add(label);
            if (labels.size() >= ROUTE_LANDMARK_MAX_COUNT) {
                break;
            }
        }
        String landmarkText = labels.isEmpty()
                ? "经过地标：高德未返回明显沿线地标"
                : "经过地标：" + TextUtils.join("、", labels);
        for (RouteLandmark landmark : landmarks) {
            for (LocationDto location : locations) {
                if (location != null && landmark.label.equals(location.name)) {
                    activeRouteWaypointIds.add(location.id);
                }
            }
        }
        String summary = tvRouteSummary == null || TextUtils.isEmpty(tvRouteSummary.getText())
                ? getString(R.string.label_route_detail_heading)
                : tvRouteSummary.getText().toString();
        setRouteDetailUi(summary, landmarkText + "\n" + roadText);
    }

    private List<GeoPoint> sampleRoutePoints(@NonNull List<GeoPoint> routePoints, int maxCount) {
        List<GeoPoint> samples = new ArrayList<>();
        if (routePoints.isEmpty() || maxCount <= 0) {
            return samples;
        }
        if (routePoints.size() <= maxCount) {
            samples.addAll(routePoints);
            return samples;
        }
        int count = Math.min(maxCount, routePoints.size());
        for (int i = 0; i < count; i++) {
            int index = Math.round(i * (routePoints.size() - 1) / (float) (count - 1));
            GeoPoint point = routePoints.get(index);
            if (point != null) {
                samples.add(point);
            }
        }
        return samples;
    }

    private boolean isUsefulAmapRouteLandmark(@Nullable String label, @Nullable String typeDesc) {
        String text = label == null ? "" : label.trim();
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        String type = typeDesc == null ? "" : typeDesc;
        return !text.contains("停车场")
                && !text.contains("出入口")
                && !text.contains("售票")
                && !text.contains("卫生间")
                && !text.contains("公共厕所")
                && !text.endsWith("路")
                && !type.contains("道路")
                && !type.contains("门牌信息");
    }

    private boolean containsNormalizedLabel(@NonNull List<String> labels, @NonNull String candidate) {
        String normalized = normalizeSearchText(candidate);
        for (String label : labels) {
            String existing = normalizeSearchText(label);
            if (existing.equals(normalized)
                    || existing.contains(normalized)
                    || normalized.contains(existing)) {
                return true;
            }
        }
        return false;
    }

    private RouteNearestPoint findNearestRoutePoint(@NonNull GeoPoint point,
                                                    @NonNull List<GeoPoint> routePoints) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < routePoints.size(); i++) {
            GeoPoint routePoint = routePoints.get(i);
            if (routePoint == null) {
                continue;
            }
            double distance = calculateDistanceMeters(
                    point.getLatitude(),
                    point.getLongitude(),
                    routePoint.getLatitude(),
                    routePoint.getLongitude());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return new RouteNearestPoint(bestIndex, bestDistance);
    }

    private void handleRouteNavigationAutoPlay(@NonNull CheckInDto data) {
        if (data.triggeredLocations == null || data.triggeredLocations.isEmpty()) {
            return;
        }
        for (TriggeredLocationDto triggered : data.triggeredLocations) {
            if (triggered == null) {
                continue;
            }
            if (!activeRouteWaypointIds.contains(triggered.locationId)
                    && triggered.locationId != routedLocationId) {
                continue;
            }
            LocationDto location = findLocationById(triggered.locationId);
            if (location == null || !geoFenceManager.canAutoPlay(location)) {
                continue;
            }
            geoFenceManager.markPlayed(location.id);
            playGuide(location, true);
        }
    }

    @NonNull
    private List<LocationDto> collectBackendCampusSpotsForGeofence() {
        List<LocationDto> spots = new ArrayList<>();
        for (LocationDto location : locations) {
            if (isDatabaseCampusSpot(location) && hasPlottableLocation(location)) {
                spots.add(location);
            }
        }
        return spots;
    }

    /**
     * GPS 更新时本地围栏判定：即时触发自动讲解，弱网/离线可用。
     */
    private void evaluateLocalGeofencesAndAutoPlay() {
        if (geoFenceManager == null || currentRawGeoPoint == null) {
            lastLocalFenceHitCount = 0;
            return;
        }
        List<LocationDto> campusSpots = collectBackendCampusSpotsForGeofence();
        if (campusSpots.isEmpty()) {
            lastLocalFenceHitCount = 0;
            geoFenceManager.syncActiveFenceIds(new HashSet<>());
            return;
        }

        List<GeoFenceManager.LocalFenceHit> hits = geoFenceManager.evaluateLocalHits(
                currentRawGeoPoint,
                campusSpots,
                DEFAULT_RADIUS_METERS,
                this::resolveDisplayPoint);
        lastLocalFenceHitCount = hits.size();

        Set<Long> insideIds = new HashSet<>();
        for (GeoFenceManager.LocalFenceHit hit : hits) {
            insideIds.add(hit.location.id);
        }
        geoFenceManager.syncActiveFenceIds(insideIds);

        if (!autoGuideEnabled || hits.isEmpty()) {
            return;
        }
        if (routePolyline != null) {
            handleLocalRouteNavigationAutoPlay(hits);
        } else {
            handleLocalGeofenceAutoPlay(hits.get(0));
        }
    }

    private void handleLocalRouteNavigationAutoPlay(@NonNull List<GeoFenceManager.LocalFenceHit> hits) {
        for (GeoFenceManager.LocalFenceHit hit : hits) {
            if (!activeRouteWaypointIds.contains(hit.location.id)
                    && hit.location.id != routedLocationId) {
                continue;
            }
            if (!geoFenceManager.canAutoPlay(hit.location)) {
                continue;
            }
            geoFenceManager.markPlayed(hit.location.id);
            playGuide(hit.location, true);
        }
    }

    private void handleLocalGeofenceAutoPlay(@NonNull GeoFenceManager.LocalFenceHit hit) {
        if (!geoFenceManager.canAutoPlay(hit.location)) {
            return;
        }
        geoFenceManager.markPlayed(hit.location.id);
        selectLocation(hit.location, hit.distanceMeters, false, shouldAutoMoveCamera());
        playGuide(hit.location, true);
    }

    private void performCheckIn() {
        if (currentRawGeoPoint == null || !session.isLoggedIn()) {
            return;
        }
        GeoPoint backendPoint = toBackendCoordinate(currentRawGeoPoint);
        if (backendPoint == null) {
            return;
        }
        CheckInReq req = new CheckInReq(backendPoint.getLatitude(), backendPoint.getLongitude());
        api.checkIn(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<CheckInDto>> call, Response<ApiResponse<CheckInDto>> response) {
                runOnUiThreadIfAlive(() -> {
                    ApiResponse<CheckInDto> body = response.body();
                    if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                        return;
                    }

                    CheckInDto data = body.data;
                    geoFenceManager.syncActiveFences(data.triggeredLocations);

                    // 本地 GPS 已负责自动讲解时，服务端仅同步围栏与访问记录，避免重复播报。
                    if (backendLocationsLoaded && !collectBackendCampusSpotsForGeofence().isEmpty()) {
                        return;
                    }

                    int hitCount = data.triggeredLocations == null ? 0 : data.triggeredLocations.size();
                    updateStatus(hitCount > 0
                            ? "地图实时状态：已进入" + hitCount + "个围栏区域"
                            : "地图实时状态：当前不在围栏区域");

                    if (routePolyline != null && autoGuideEnabled) {
                        handleRouteNavigationAutoPlay(data);
                    } else {
                        boolean blockAutoPlay = keepMapOnUserResult;
                        geoFenceManager.handleCheckInResult(data, blockAutoPlay, (location, distance) -> {
                            if (!isUiAlive()) {
                                return;
                            }
                            selectLocation(location, distance, false, shouldAutoMoveCamera());
                            playGuide(location, true);
                        });
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<CheckInDto>> call, Throwable throwable) {
                // Ignore polling failure to avoid noisy UI.
            }
        });
    }

    private void playGuide(LocationDto location, boolean auto) {
        if (location == null || audioTriggerService == null) {
            return;
        }
        audioTriggerService.playGuide(location, auto);
    }

    private void stopAudio() {
        if (audioTriggerService != null) {
            audioTriggerService.stopGuide();
        }
    }

    private void resetManualGuideIfLocationChanged(long locationId) {
        if (manualGuideLocationId != -1L && manualGuideLocationId != locationId) {
            if (audioTriggerService != null) {
                audioTriggerService.stopGuide();
            }
            manualGuideLocationId = -1L;
        }
    }

    /**
     * 气泡「讲解」：同一景点奇数次开始、偶数次停止；换景点后重新从第一次开始。
     */
    private void toggleGuideForLocation(@NonNull LocationDto location) {
        holdUserMapResult();
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, location);
        selectLocation(location, distance, false, false, false);
        focusLocation(location);
        setBottomPanelCollapsed(false, true);

        if (manualGuideLocationId != location.id) {
            resetManualGuideIfLocationChanged(location.id);
            manualGuideLocationId = location.id;
            startGuidePlayback(location);
            return;
        }

        if (audioTriggerService != null && audioTriggerService.isGuidePlayingFor(location.id)) {
            audioTriggerService.stopGuide();
            toast(getString(R.string.toast_guide_stopped));
            return;
        }

        startGuidePlayback(location);
    }

    private void startGuidePlayback(@NonNull LocationDto location) {
        if (location.id >= AMAP_SEARCH_LOCATION_ID_BASE) {
            playGuide(location, false);
            toast(getString(R.string.toast_guide_started, location.name));
            return;
        }
        GeoPoint backendPoint = toBackendCoordinate(currentRawGeoPoint);
        Double lat = backendPoint == null ? null : backendPoint.getLatitude();
        Double lng = backendPoint == null ? null : backendPoint.getLongitude();
        api.manualPlay(location.id, new ManualPlayReq(lat, lng)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                playGuide(location, false);
                toast(getString(R.string.toast_guide_started, location.name));
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                playGuide(location, false);
                toast(getString(R.string.toast_guide_started, location.name));
            }
        });
    }

    @Override
    public void showToast(@NonNull String message) {
        toast(message);
    }

    private void updateCurrentLocation(@NonNull AMapLocation location) {
        if (!isMapReady()) {
            return;
        }
        if (Math.abs(location.getLatitude()) < 0.0001 && Math.abs(location.getLongitude()) < 0.0001) {
            return;
        }
        if (location.getTime() > 0 && System.currentTimeMillis() - location.getTime() > 10 * 60 * 1000L) {
            return;
        }

        boolean lowAccuracy = false;
        if (location.hasAccuracy()) {
            float limit = currentRawGeoPoint == null ? FIRST_FIX_MAX_ACCURACY_METERS : CONTINUOUS_MAX_ACCURACY_METERS;
            lowAccuracy = location.getAccuracy() > limit;
        }

        long now = System.currentTimeMillis();
        GeoPoint incomingRaw = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (currentRawGeoPoint != null && now - lastLocationUpdateTimeMs < LOCATION_JUMP_FILTER_WINDOW_MS) {
            double jumpMeters = calculateDistanceMeters(
                    currentRawGeoPoint.getLatitude(),
                    currentRawGeoPoint.getLongitude(),
                    incomingRaw.getLatitude(),
                    incomingRaw.getLongitude()
            );
            boolean highConfidence = location.hasAccuracy() && location.getAccuracy() <= LOCATION_JUMP_ACCEPT_ACCURACY_METERS;
            if (jumpMeters > LOCATION_JUMP_FILTER_METERS && !highConfidence) {
                return;
            }
        }

        currentRawGeoPoint = incomingRaw;
        lastLocationUpdateTimeMs = now;
        currentGeoPoint = locationPointToMapDisplay(location);
        if (userMarker == null && campusMapManager != null && campusMapManager.getMap() != null) {
            userMarker = campusMapManager.getMap().addMarker(new MarkerOptions()
                    .position(CampusMapManager.toLatLng(currentGeoPoint))
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (userMarker != null) {
                userMarker.setClickable(false);
            }
        }
        if (userMarker != null) {
            userMarker.setPosition(CampusMapManager.toLatLng(currentGeoPoint));
        }

        if (trackManager != null) {
            trackManager.appendPoint(currentGeoPoint);
        }
        refreshNearbyCards();

        if (selectedLocation != null) {
            double distance = distanceRawToLocationMeters(currentRawGeoPoint, selectedLocation);
            selectLocation(selectedLocation, distance, false, false);
        }

        evaluateLocalGeofencesAndAutoPlay();

        boolean inCampusNow = isInCampusSmart(currentRawGeoPoint);
        if (shouldAutoMoveCamera()) {
            if (!hasAutoCentered) {
                hasAutoCentered = true;
                if (routePolyline == null && mapCameraMode == MapCameraMode.FREE) {
                    applyDefaultMapAnchor(true);
                }
            } else if (followMyLocation) {
                campusMapManager.setCenterAndZoom(currentGeoPoint, FOLLOW_CAMERA_ZOOM);
            }
        } else {
            hasAutoCentered = true;
        }

        String accuracyText = location.hasAccuracy()
                ? String.format(Locale.CHINA, "±%.0f米", location.getAccuracy())
                : "未知";
        boolean inCampus = isInCampusSmart(currentRawGeoPoint);
        double campusDistanceMeters = calculateCampusDistanceMeters(currentRawGeoPoint);
        String areaText;
        if (inCampus) {
            areaText = "在西南大学校园内";
        } else if (campusDistanceMeters > 50_000) {
            areaText = "在西南大学校园外（模拟器请设置北碚校区定位）";
        } else {
            areaText = String.format(Locale.CHINA, "在西南大学校园外（%.1f公里）", campusDistanceMeters / 1000.0);
        }
        String fenceText = lastLocalFenceHitCount > 0
                ? "已进入" + lastLocalFenceHitCount + "个围栏区域"
                : "当前不在围栏区域";
        if (lowAccuracy) {
            updateStatus("地图实时状态：" + areaText + "，" + fenceText + "（定位精度较低，" + accuracyText + "）");
        } else {
            updateStatus("地图实时状态：" + areaText + "，" + fenceText + "，定位精度" + accuracyText);
        }
        campusMapManager.invalidate();
    }

    private void centerToCurrent() {
        unlockMapCameraForFollow();
        if (currentGeoPoint == null) {
            requestSinglePreciseLocation();
            toast(getString(R.string.toast_locating));
            return;
        }
        requestSinglePreciseLocation();
        followMyLocation = true;
        campusMapManager.setCenterAndZoom(currentGeoPoint, FOLLOW_CAMERA_ZOOM);
        boolean inCampusNow = currentRawGeoPoint != null && isInCampusSmart(currentRawGeoPoint);
        toast(inCampusNow
                ? getString(R.string.toast_centered_on_location)
                : getString(R.string.toast_outside_campus_locate));
    }

    private void zoomInMap() {
        campusMapManager.zoomIn();
    }

    private void zoomOutMap() {
        campusMapManager.zoomOut();
    }

    private void focusCampusOverview() {
        applyDefaultMapAnchor(true);
    }

    private void applyDefaultMapAnchor(boolean animated) {
        if (!isMapReady()) {
            return;
        }
        mapCameraMode = MapCameraMode.FREE;
        lockedSelectionPoint = null;
        lockedRouteBounds = null;
        followMyLocation = false;
        keepMapOnUserResult = false;
        defaultMapAnchorApplied = true;
        if (animated) {
            campusMapManager.focusCampusOverview(true);
        } else {
            campusMapManager.applyMapAnchorImmediate();
        }
    }

    /** 开始重新规划：仅清除旧折线，保持校园默认视野。 */
    private void beginRoutePlanning() {
        routeDrawGeneration++;
        clearRoutePolylineOnly();
        lastRoutePolylinePoints.clear();
        lastAmapRouteSteps.clear();
        activeRouteWaypointIds.clear();
        if (routeDirectionsAdapter != null) {
            routeDirectionsAdapter.submit(new ArrayList<>());
        }
        if (routeNamedPlacesAdapter != null) {
            routeNamedPlacesAdapter.submit(new ArrayList<>());
        }
    }

    private GeoPoint resolveCampusRouteStartPoint() {
        long startSpotId = parseRouteEndLocId(selectedRouteStartId);
        if (startSpotId > 0) {
            LocationDto startSpot = findLocationById(startSpotId);
            GeoPoint spotPoint = startSpot == null ? null : resolveDisplayPoint(startSpot);
            if (spotPoint != null) {
                return spotPoint;
            }
        }
        if (!ROUTE_START_CURRENT.equals(selectedRouteStartId) && !TextUtils.isEmpty(selectedRouteStartId)) {
            GeoPoint node = campusRouteNodes.get(selectedRouteStartId);
            if (node != null) {
                return node;
            }
        }
        if (currentGeoPoint == null) {
            return getCampusCenterPointForMap();
        }
        if (currentRawGeoPoint != null && isInCampusSmart(currentRawGeoPoint)) {
            return currentGeoPoint;
        }
        GeoPoint southGate = campusRouteNodes.get("SWU_GATE_S");
        if (southGate != null) {
            return southGate;
        }
        return getCampusCenterPointForMap();
    }

    @Nullable
    private GeoPoint resolveRouteEndPoint(@NonNull LocationDto destination) {
        return resolveDisplayPoint(destination);
    }

    private static String routeEndLocId(long locationId) {
        return ROUTE_END_LOC_PREFIX + locationId;
    }

    private long parseRouteEndLocId(@Nullable String routeEndId) {
        if (routeEndId == null || !routeEndId.startsWith(ROUTE_END_LOC_PREFIX)) {
            return -1L;
        }
        try {
            return Long.parseLong(routeEndId.substring(ROUTE_END_LOC_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private boolean hasPlottableLocation(@NonNull LocationDto location) {
        return resolveDisplayPoint(location) != null
                || isValidLatLng(location.latitude, location.longitude);
    }

    private void showNearestSpotsPanel() {
        if (currentRawGeoPoint == null || locations.isEmpty()) {
            toast(getString(R.string.toast_locating));
            return;
        }
        setBottomPanelCollapsed(false, true);
        if (nearbySection != null) {
            nearbySection.setVisibility(View.VISIBLE);
        }
        refreshNearbyCards();
    }

    private void openLocationOnMap(long locationId) {
        setActiveNavTab(0);
        for (LocationDto location : locations) {
            if (location.id == locationId) {
                Double distance = currentRawGeoPoint == null
                        ? null
                        : distanceRawToLocationMeters(currentRawGeoPoint, location);
                holdUserMapResult();
                clearRouteIfDestinationChanged(location.id);
                selectLocation(location, distance);
                focusLocation(location);
                if (!hasPlottableLocation(location)) {
                    requestAmapPoiCoordinateFix(location, true);
                }
                return;
            }
        }
        toast(getString(R.string.toast_location_not_found));
    }

    private void updateCampusSnapshotStatus() {
        if (locations.isEmpty()) {
            return;
        }
        int total = locations.size();
        int withAudio = 0;
        for (LocationDto location : locations) {
            if (!TextUtils.isEmpty(location.audioUrl)) {
                withAudio++;
            }
        }
        String mapMode = isGcjMapLayerActive() ? "GCJ图层" : "WGS图层";
        updateStatus("地图实时状态：西南大学点位" + total + "个，音频" + withAudio + "个，当前" + mapMode);
    }

    @Nullable
    private GeoPoint resolveDisplayPoint(@Nullable LocationDto location) {
        if (location == null) {
            return null;
        }
        // Web 管理端（Leaflet + 高德瓦片）与 Android 高德 SDK 均使用 GCJ-02，后端坐标直接用于底图。
        boolean backendLocation = location.id > 0 && location.id < AMAP_SEARCH_LOCATION_ID_BASE;
        if (backendLocation && isValidLatLng(location.latitude, location.longitude)) {
            return poiToMapPoint(location.latitude, location.longitude);
        }
        GeoPoint override = locationCoordinateOverrides.get(location.id);
        if (override != null) {
            return override;
        }
        GeoPoint cached = locationDisplayPoints.get(location.id);
        if (cached != null) {
            return cached;
        }
        if (isValidLatLng(location.latitude, location.longitude)) {
            return poiToMapPoint(location.latitude, location.longitude);
        }
        return null;
    }

    private void rebuildLocationDisplayPoints() {
        locationDisplayPoints.clear();
        poiRouteNodeHints.clear();
        for (LocationDto location : locations) {
            if (!isValidLatLng(location.latitude, location.longitude)) {
                continue;
            }
            GeoPoint point = poiToMapPoint(location.latitude, location.longitude);
            locationDisplayPoints.put(location.id, point);
            String nearest = findNearestRouteNode(point);
            if (!TextUtils.isEmpty(nearest)) {
                poiRouteNodeHints.put(location.id, nearest);
            }
        }
        for (Map.Entry<Long, GeoPoint> entry : locationCoordinateOverrides.entrySet()) {
            locationDisplayPoints.put(entry.getKey(), entry.getValue());
            String nearest = findNearestRouteNode(entry.getValue());
            if (!TextUtils.isEmpty(nearest)) {
                poiRouteNodeHints.put(entry.getKey(), nearest);
            }
        }
    }

    private boolean isGcjMapLayerActive() {
        return campusMapManager.isGcjMapLayerActive();
    }

    private GeoPoint getCampusCenterPointForMap() {
        return CampusMapManager.getMapAnchorPoint();
    }

    @NonNull
    private GeoPoint locationPointToMapDisplay(@NonNull AMapLocation location) {
        if (location.getCoordType() == AMapLocation.COORD_TYPE_GCJ02) {
            return new GeoPoint(location.getLatitude(), location.getLongitude());
        }
        return gpsToMapPoint(location.getLatitude(), location.getLongitude());
    }

    private GeoPoint poiToMapPoint(double gcjLat, double gcjLng) {
        return CoordinateUtils.poiToMapDisplay(gcjLat, gcjLng, isGcjMapLayerActive());
    }

    private GeoPoint gpsToMapPoint(double wgsLat, double wgsLng) {
        return CoordinateUtils.gpsToMapDisplay(wgsLat, wgsLng, isGcjMapLayerActive());
    }

    private double distanceRawToLocationMeters(@Nullable GeoPoint rawGcjPoint, @NonNull LocationDto location) {
        if (rawGcjPoint == null) {
            return Double.NaN;
        }
        GeoPoint display = resolveDisplayPoint(location);
        if (display == null) {
            return DistanceUtil.distanceRawToDisplayPoint(
                    rawGcjPoint, location, locationCoordinateOverrides);
        }
        return DistanceUtil.distanceMeters(rawGcjPoint, display);
    }

    @Nullable
    private GeoPoint toBackendCoordinate(@Nullable GeoPoint rawGcjPoint) {
        if (rawGcjPoint == null) {
            return null;
        }
        return new GeoPoint(rawGcjPoint.getLatitude(), rawGcjPoint.getLongitude());
    }

    private double calculateCampusDistanceMeters(@Nullable GeoPoint rawGcjPoint) {
        if (rawGcjPoint == null) {
            return Double.MAX_VALUE;
        }
        return calculateDistanceMeters(rawGcjPoint.getLatitude(), rawGcjPoint.getLongitude(),
                CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG);
    }

    private boolean isInCampusSmart(@Nullable GeoPoint rawGcjPoint) {
        if (rawGcjPoint == null) {
            return false;
        }
        if (isInCampus(rawGcjPoint.getLatitude(), rawGcjPoint.getLongitude())) {
            return true;
        }
        return calculateCampusDistanceMeters(rawGcjPoint) <= CAMPUS_INSIDE_RADIUS_METERS;
    }

    private String normalizeSearchText(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(" ", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private void initCampusRouteGraph() {
        campusRouteNodes.clear();
        campusRouteNodeLabels.clear();
        campusRouteGraph.clear();

        addRouteNode("SWU_GATE_S", "南门", 29.8206700, 106.4236000);
        addRouteNode("SWU_GATE_HUB", "南门广场", 29.8212000, 106.4229000);
        addRouteNode("SWU_MAIN_E", "主干道东段", 29.8225000, 106.4215000);
        addRouteNode("SWU_TEACH_25", "25教学楼", 29.8251130, 106.4202302);
        addRouteNode("SWU_LIBRARY", "中心图书馆", 29.8230614, 106.4204318);
        addRouteNode("SWU_CSIS", "计算机与信息科学学院报告厅", 29.8244300, 106.4184100);
        addRouteNode("SWU_SPORT_2", "第二运动场", 29.8269500, 106.4220100);
        addRouteNode("SWU_GEO", "地理科学学院", 29.8241302, 106.4189186);
        addRouteNode("SWU_SPORT_3", "第三运动场", 29.8262060, 106.4185040);
        addRouteNode("SWU_GRAD", "研究生院", 29.8208890, 106.4176230);
        addRouteNode("SWU_WEST_CONN", "西侧连廊", 29.82190, 106.41830);
        addRouteNode("SWU_NORTH_CONN", "北侧连接点", 29.82385, 106.42090);

        connectRouteNodes("SWU_GATE_S", "SWU_GATE_HUB");
        connectRouteNodes("SWU_GATE_HUB", "SWU_MAIN_E");
        connectRouteNodes("SWU_MAIN_E", "SWU_GEO");
        connectRouteNodes("SWU_GEO", "SWU_TEACH_25");
        connectRouteNodes("SWU_TEACH_25", "SWU_LIBRARY");
        connectRouteNodes("SWU_LIBRARY", "SWU_CSIS");
        connectRouteNodes("SWU_CSIS", "SWU_SPORT_2");
        connectRouteNodes("SWU_TEACH_25", "SWU_WEST_CONN");
        connectRouteNodes("SWU_WEST_CONN", "SWU_GRAD");
        connectRouteNodes("SWU_GEO", "SWU_SPORT_3");
        connectRouteNodes("SWU_MAIN_E", "SWU_SPORT_3");
        connectRouteNodes("SWU_GEO", "SWU_NORTH_CONN");
        connectRouteNodes("SWU_NORTH_CONN", "SWU_LIBRARY");
        connectRouteNodes("SWU_TEACH_25", "SWU_MAIN_E");
    }

    /** 将后端数据库景点并入校内路网图，供迪杰斯特拉规划使用。 */
    private void mergeBackendLocationsIntoRouteGraph() {
        for (LocationDto location : locations) {
            if (location == null || !hasPlottableLocation(location)) {
                continue;
            }
            GeoPoint display = resolveDisplayPoint(location);
            if (display == null) {
                continue;
            }
            String nodeId = "LOC_" + location.id;
            if (!campusRouteNodes.containsKey(nodeId)) {
                addRouteNodeFromDisplay(nodeId, location.name, display);
                String nearest = findNearestRouteNode(display);
                if (!TextUtils.isEmpty(nearest) && !nearest.equals(nodeId)) {
                    connectRouteNodes(nodeId, nearest);
                }
            }
            poiRouteNodeHints.put(location.id, nodeId);
        }
    }

    private void addRouteNodeFromDisplay(@NonNull String nodeId,
                                         @NonNull String label,
                                         @NonNull GeoPoint displayPoint) {
        campusRouteNodes.put(nodeId, displayPoint);
        campusRouteNodeLabels.put(nodeId, label);
        campusRouteGraph.put(nodeId, new ArrayList<>());
    }

    private void addRouteNode(String nodeId, String label, double lat, double lng) {
        GeoPoint point = poiToMapPoint(lat, lng);
        campusRouteNodes.put(nodeId, point);
        campusRouteNodeLabels.put(nodeId, label);
        campusRouteGraph.put(nodeId, new ArrayList<>());
    }

    private void connectRouteNodes(String a, String b) {
        List<String> aList = campusRouteGraph.get(a);
        List<String> bList = campusRouteGraph.get(b);
        if (aList == null || bList == null) {
            return;
        }
        if (!aList.contains(b)) {
            aList.add(b);
        }
        if (!bList.contains(a)) {
            bList.add(a);
        }
    }

    @Nullable
    private String resolveRouteStartGraphNode(@NonNull GeoPoint start) {
        long startSpotId = parseRouteEndLocId(selectedRouteStartId);
        if (startSpotId > 0) {
            String nodeId = routeLocationNodeId(startSpotId);
            if (campusRouteNodes.containsKey(nodeId)) {
                return nodeId;
            }
        }
        return findNearestRouteNode(start);
    }

    @Nullable
    private String resolveRouteEndGraphNode(@NonNull GeoPoint end, @Nullable LocationDto destination) {
        long endSpotId = parseRouteEndLocId(selectedRouteEndId);
        if (endSpotId > 0) {
            String nodeId = routeLocationNodeId(endSpotId);
            if (campusRouteNodes.containsKey(nodeId)) {
                return nodeId;
            }
        }
        if (destination != null) {
            String hinted = poiRouteNodeHints.get(destination.id);
            if (!TextUtils.isEmpty(hinted) && campusRouteNodes.containsKey(hinted)) {
                return hinted;
            }
        }
        return findNearestRouteNode(end);
    }

    @NonNull
    private static String routeLocationNodeId(long locationId) {
        return "LOC_" + locationId;
    }

    private List<GeoPoint> buildCampusRoute(GeoPoint start, GeoPoint end, @Nullable LocationDto destination) {
        List<GeoPoint> points = new ArrayList<>();
        points.add(start);
        if (campusRouteNodes.isEmpty()) {
            points.add(end);
            return points;
        }

        String startNode = resolveRouteStartGraphNode(start);
        String endNode = resolveRouteEndGraphNode(end, destination);

        if (TextUtils.isEmpty(startNode) || TextUtils.isEmpty(endNode)) {
            points.add(end);
            return points;
        }

        if (startNode.equals(endNode)) {
            points.add(end);
            return points;
        }

        List<String> nodePath = shortestNodePath(startNode, endNode);
        if (nodePath.isEmpty()) {
            points.add(end);
            return points;
        }

        GeoPoint firstNodePoint = campusRouteNodes.get(nodePath.get(0));
        if (firstNodePoint != null && calculateDistanceMeters(start.getLatitude(), start.getLongitude(),
                firstNodePoint.getLatitude(), firstNodePoint.getLongitude()) > 6) {
            points.add(firstNodePoint);
        }

        for (String nodeId : nodePath) {
            GeoPoint nodePoint = campusRouteNodes.get(nodeId);
            if (nodePoint == null) {
                continue;
            }
            GeoPoint lastPoint = points.get(points.size() - 1);
            if (calculateDistanceMeters(lastPoint.getLatitude(), lastPoint.getLongitude(),
                    nodePoint.getLatitude(), nodePoint.getLongitude()) >= 2) {
                points.add(nodePoint);
            }
        }

        GeoPoint last = points.get(points.size() - 1);
        if (calculateDistanceMeters(last.getLatitude(), last.getLongitude(),
                end.getLatitude(), end.getLongitude()) >= 2) {
            points.add(end);
        }
        return points;
    }

    @Nullable
    private String findNearestRouteNode(@Nullable GeoPoint point) {
        if (point == null || campusRouteNodes.isEmpty()) {
            return null;
        }
        String best = null;
        double minDistance = Double.MAX_VALUE;
        for (Map.Entry<String, GeoPoint> entry : campusRouteNodes.entrySet()) {
            GeoPoint nodePoint = entry.getValue();
            double distance = calculateDistanceMeters(point.getLatitude(), point.getLongitude(),
                    nodePoint.getLatitude(), nodePoint.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                best = entry.getKey();
            }
        }
        return best;
    }

    private List<String> shortestNodePath(String start, String end) {
        if (TextUtils.isEmpty(start) || TextUtils.isEmpty(end)) {
            return Collections.emptyList();
        }
        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String node : campusRouteNodes.keySet()) {
            dist.put(node, Double.MAX_VALUE);
        }
        dist.put(start, 0d);

        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                Comparator.comparingDouble(it -> it.distance));
        queue.add(new NodeDistance(start, 0d));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.distance > dist.getOrDefault(current.nodeId, Double.MAX_VALUE)) {
                continue;
            }
            if (current.nodeId.equals(end)) {
                break;
            }
            List<String> neighbors = campusRouteGraph.get(current.nodeId);
            if (neighbors == null) {
                continue;
            }
            for (String next : neighbors) {
                GeoPoint from = campusRouteNodes.get(current.nodeId);
                GeoPoint to = campusRouteNodes.get(next);
                if (from == null || to == null) {
                    continue;
                }
                double weight = calculateDistanceMeters(from.getLatitude(), from.getLongitude(),
                        to.getLatitude(), to.getLongitude());
                double candidate = current.distance + weight;
                if (candidate < dist.getOrDefault(next, Double.MAX_VALUE)) {
                    dist.put(next, candidate);
                    prev.put(next, current.nodeId);
                    queue.add(new NodeDistance(next, candidate));
                }
            }
        }

        if (!prev.containsKey(end)) {
            return Collections.singletonList(start);
        }
        List<String> path = new ArrayList<>();
        String at = end;
        path.add(at);
        while (prev.containsKey(at)) {
            at = prev.get(at);
            path.add(at);
            if (at.equals(start)) {
                break;
            }
        }
        Collections.reverse(path);
        return path;
    }

    private double calculatePathDistanceMeters(List<GeoPoint> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }
        double distance = 0;
        for (int i = 1; i < points.size(); i++) {
            GeoPoint a = points.get(i - 1);
            GeoPoint b = points.get(i);
            distance += calculateDistanceMeters(a.getLatitude(), a.getLongitude(),
                    b.getLatitude(), b.getLongitude());
        }
        return distance;
    }

    private void applyRouteMode(@NonNull RouteMode mode, boolean refreshRouteIfNeeded) {
        routeMode = mode;
        applyRouteChipState(chipRouteWalk, mode == RouteMode.WALK);
        applyRouteChipState(chipRouteDrive, mode == RouteMode.DRIVE);
        applyRouteChipState(chipRouteBus, mode == RouteMode.BUS);
        applyRouteChipState(chipRouteTaxi, mode == RouteMode.TAXI);

        if (refreshRouteIfNeeded && resolveRouteDestination() != null) {
            showRouteToSelected();
            return;
        }
        if (routePolyline != null) {
            return;
        }
        LocationDto routeEnd = resolveRouteDestination();
        if (routeEnd != null && currentRawGeoPoint != null) {
            double distance = distanceRawToLocationMeters(currentRawGeoPoint, routeEnd);
            updateRoutePreviewText(distance);
        } else if (tvRouteSteps != null) {
            tvRouteSteps.setText(getString(R.string.label_route_preview_tip, getRouteModeLabel(routeMode)));
        }
    }

    private void applyRouteChipState(@Nullable TextView chip, boolean active) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(active ? R.drawable.bg_nav_active : R.drawable.bg_service_pill);
        chip.setTextColor(ContextCompat.getColor(this, active ? R.color.brand_primary_dark : R.color.text_main));
    }

    private void updateRoutePreviewText(double directDistanceMeters) {
        if (directDistanceMeters > 50000) {
            tvRouteSummary.setText(R.string.label_far_from_campus);
            tvRouteSteps.setText(getString(R.string.label_route_far_tip));
            return;
        }
        int minutes = estimateRouteMinutes(directDistanceMeters);
        String modeName = getRouteModeLabel(routeMode);
        tvRouteSummary.setText(getString(R.string.label_estimate_time_mode, modeName, minutes, directDistanceMeters / 1000.0));
        tvRouteSteps.setText(getString(R.string.label_route_preview_tip, modeName));
    }

    private int estimateRouteMinutes(double distanceMeters) {
        double speedMps = getRouteSpeed(routeMode);
        return Math.max(CAMPUS_ROUTE_MINUTES_BIAS, (int) Math.ceil(distanceMeters / speedMps / 60.0));
    }

    private double getRouteSpeed(@NonNull RouteMode mode) {
        switch (mode) {
            case WALK:
                return CAMPUS_ROUTE_WALK_MPS;
            case DRIVE:
                return CAMPUS_ROUTE_DRIVE_MPS;
            case BUS:
                return CAMPUS_ROUTE_BUS_MPS;
            case TAXI:
                return CAMPUS_ROUTE_TAXI_MPS;
            default:
                return CAMPUS_ROUTE_WALK_MPS;
        }
    }

    private int getRouteColor(@NonNull RouteMode mode) {
        switch (mode) {
            case BUS:
                return ContextCompat.getColor(this, R.color.route_bus_line);
            case TAXI:
                return ContextCompat.getColor(this, R.color.route_taxi_line);
            case DRIVE:
                return ContextCompat.getColor(this, R.color.route_drive_line);
            case WALK:
            default:
                return ContextCompat.getColor(this, R.color.route_walk_line);
        }
    }

    private String getRouteModeLabel(@NonNull RouteMode mode) {
        switch (mode) {
            case WALK:
                return getString(R.string.service_walk);
            case DRIVE:
                return getString(R.string.service_drive);
            case BUS:
                return getString(R.string.service_bus);
            case TAXI:
                return getString(R.string.service_taxi);
            default:
                return getString(R.string.service_walk);
        }
    }

    private void openShareHub(@Nullable String category) {
        if (!SessionManager.from(this).isLoggedIn()) {
            toast(getString(R.string.toast_feedback_need_login));
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        Intent intent = new Intent(this, ShareHubActivity.class);
        if (category != null) {
            intent.putExtra(ShareHubActivity.EXTRA_CATEGORY, category);
        }
        startActivity(intent);
    }

    private String describeRouteNodes(List<GeoPoint> routePoints, @Nullable LocationDto destination) {
        if (destination == null) {
            return "校园路线";
        }
        String node = poiRouteNodeHints.get(destination.id);
        if (TextUtils.isEmpty(node)) {
            node = findNearestRouteNode(routePoints == null || routePoints.isEmpty() ? null : routePoints.get(routePoints.size() - 1));
        }
        String label = TextUtils.isEmpty(node) ? "" : campusRouteNodeLabels.get(node);
        if (TextUtils.isEmpty(label)) {
            return destination.name;
        }
        return label + " -> " + destination.name;
    }

    private String buildRouteStepText(@Nullable GeoPoint start, @Nullable GeoPoint end,
                                      @Nullable LocationDto destination, @NonNull String fallbackHint) {
        if (start == null || end == null) {
            return getString(R.string.label_route_hint, fallbackHint);
        }

        String startNode = findNearestRouteNode(start);
        String endNode = destination == null ? findNearestRouteNode(end) : poiRouteNodeHints.get(destination.id);
        if (TextUtils.isEmpty(endNode)) {
            endNode = findNearestRouteNode(end);
        }

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.label_route_start));
        if (!TextUtils.isEmpty(startNode) && !TextUtils.isEmpty(endNode)) {
            List<String> nodePath = shortestNodePath(startNode, endNode);
            for (String nodeId : nodePath) {
                String label = campusRouteNodeLabels.get(nodeId);
                if (TextUtils.isEmpty(label)) {
                    continue;
                }
                if (!labels.isEmpty() && label.equals(labels.get(labels.size() - 1))) {
                    continue;
                }
                labels.add(label);
            }
        }

        String destinationName = destination != null && !TextUtils.isEmpty(destination.name)
                ? destination.name
                : getString(R.string.label_destination);
        if (labels.isEmpty() || !destinationName.equals(labels.get(labels.size() - 1))) {
            labels.add(destinationName);
        }

        if (labels.size() <= 2) {
            return getString(R.string.label_route_hint, fallbackHint);
        }
        return getString(R.string.label_route_steps_format, TextUtils.join(" -> ", labels));
    }

    private String buildGuideNarration(@Nullable LocationDto location) {
        if (location == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(location.name).append("。");
        if (!TextUtils.isEmpty(location.category)) {
            builder.append("分类：").append(location.category).append("。");
        }
        if (!TextUtils.isEmpty(location.description)) {
            builder.append(location.description).append("。");
        }
        if (!TextUtils.isEmpty(location.address)) {
            builder.append("地址：").append(location.address).append("。");
        }

        List<LocationDto> nearest = findNearestPoisOf(location, 2);
        if (!nearest.isEmpty()) {
            builder.append("附近推荐：");
            for (int i = 0; i < nearest.size(); i++) {
                if (i > 0) {
                    builder.append("、");
                }
                builder.append(nearest.get(i).name);
            }
            builder.append("。");
        }
        return builder.toString();
    }
    private List<LocationDto> findNearestPoisOf(LocationDto source, int limit) {
        if (source == null || locations.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        List<LocationDto> pool = new ArrayList<>(locations);
        pool.removeIf(it -> it == null || it.id == source.id);
        pool.sort(Comparator.comparingDouble(it -> calculateDistanceMeters(
                source.latitude, source.longitude, it.latitude, it.longitude)));
        if (pool.size() > limit) {
            return new ArrayList<>(pool.subList(0, limit));
        }
        return pool;
    }

    private void openLocationDetail(@NonNull LocationDto location) {
        if (!isUiAlive()) {
            return;
        }
        Double distance = currentRawGeoPoint == null
                ? null
                : distanceRawToLocationMeters(currentRawGeoPoint, location);
        if (distance != null && !com.example.gps.util.DistanceFormat.isUsable(distance)) {
            distance = null;
        }
        clearRouteIfDestinationChanged(location.id);
        holdUserMapResult();
        selectLocation(location, distance, false, true);
        focusLocation(location);
        setBottomPanelCollapsed(false, true);
        if (markerManager != null) {
            markerManager.setSelectedLocationId(location.id);
        }
        List<String> nearbyNames = new ArrayList<>();
        for (LocationDto nearby : findNearestPoisOf(location, 3)) {
            if (nearby != null && !TextUtils.isEmpty(nearby.name)) {
                nearbyNames.add(nearby.name);
            }
        }
        spotDetailLauncher.launch(SpotDetailActivity.buildIntent(this, location, distance, nearbyNames));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_msg)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_yes_logout, (dialog, which) -> logout())
                .show();
    }

    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        return DistanceUtil.distanceMeters(lat1, lng1, lat2, lng2);
    }

    private boolean isInCampus(double lat, double lng) {
        return CampusMapManager.isInCampusRect(lat, lng);
    }

    private void logout() {
        session.clearAuth();
        stopAudio();
        gotoLogin();
    }

    private void gotoLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureFloatOverlaysOnTop();
        if (campusMapManager != null) {
            campusMapManager.onResume();
        }
        if (routePolyline == null && lastRoutePolylinePoints.size() >= 2
                && routedLocationId >= 0 && resolveRouteDestination() != null) {
            redrawStoredRoutePolyline();
        } else if (routePolyline == null && routedLocationId < 0 && mapCameraMode == MapCameraMode.FREE) {
            applyDefaultMapAnchor(false);
        }
        reapplyMapCameraLock();
        if (PermissionUtil.hasLocationPermission(this)) {
            campusMapManager.enableMyLocationBlueDot(true);
            startLocationUpdates();
        } else {
            PermissionUtil.requestLocationPermission(this);
        }
        handler.removeCallbacks(checkInTask);
        handler.postDelayed(checkInTask, CHECK_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (campusMapManager != null) {
            campusMapManager.onPause();
        }
        stopLocationUpdates();
        handler.removeCallbacks(checkInTask);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 不保存地图镜头状态，避免下次启动偏离中心图书馆全景
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(checkInTask);
        stopLocationUpdates();
        if (audioTriggerService != null) {
            audioTriggerService.shutdown();
        }
        if (locationService != null) {
            locationService.destroy();
            locationService = null;
        }
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
        if (userMarker != null) {
            userMarker.remove();
            userMarker = null;
        }
        if (campusMapManager != null) {
            campusMapManager.onDetach();
            campusMapManager = null;
        }
        markerManager = null;
        routeSearch = null;
        super.onDestroy();
    }

    private boolean isUiAlive() {
        return !isFinishing() && !isDestroyed();
    }

    private boolean isMapReady() {
        return isUiAlive() && campusMapManager != null && campusMapManager.getMap() != null;
    }

    private void runOnUiThreadIfAlive(@NonNull Runnable action) {
        if (!isUiAlive()) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            runOnUiThread(() -> {
                if (isUiAlive()) {
                    action.run();
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (locationService == null) {
            initLocation();
        }
        if (locationService == null) {
            return;
        }
        locationService.start();
    }

    private void stopLocationUpdates() {
        if (locationService != null) {
            locationService.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (PermissionUtil.isLocationPermissionGranted(requestCode, grantResults)) {
                if (campusMapManager != null) {
                    campusMapManager.enableMyLocationBlueDot(true);
                }
                startLocationUpdates();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_location_perm_title)
                        .setMessage(R.string.dialog_location_perm_msg)
                        .setPositiveButton(R.string.btn_close, null)
                        .show();
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setActiveNavTab(int tab) {
        TextView[] tabs = {btnNavHome, btnNavAround, btnNavShare, btnNavMine};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) {
                continue;
            }
            boolean active = i == tab;
            tabs[i].setBackgroundResource(active ? R.drawable.bg_nav_active : R.drawable.bg_service_pill);
            tabs[i].setTextColor(ContextCompat.getColor(this, active ? R.color.brand_primary_dark : R.color.text_sub));
        }
    }

    @SuppressLint("MissingPermission")
    private void requestSinglePreciseLocation() {
        if (!PermissionUtil.hasLocationPermission(this)) {
            return;
        }
        startLocationUpdates();
    }

    private static class RouteLandmark {
        final String label;
        final int routeIndex;
        final double distanceMeters;

        RouteLandmark(String label, int routeIndex, double distanceMeters) {
            this.label = label;
            this.routeIndex = routeIndex;
            this.distanceMeters = distanceMeters;
        }
    }

    private static class RouteNearestPoint {
        final int routeIndex;
        final double distanceMeters;

        RouteNearestPoint(int routeIndex, double distanceMeters) {
            this.routeIndex = routeIndex;
            this.distanceMeters = distanceMeters;
        }
    }

    private static class NodeDistance {
        final String nodeId;
        final double distance;

        NodeDistance(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    private enum RouteMode {
        WALK,
        DRIVE,
        BUS,
        TAXI
    }
}
