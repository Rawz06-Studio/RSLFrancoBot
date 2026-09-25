# Plan de Division RSLFrancoBot en 3 Services

**Date:** 2026-09-14  
**Statut:** ✅ Validé - Prêt pour Phase 1  
**Stack décidée:** Java (Spring Boot) pour Services 2 & 3 | Python (FastAPI) pour Service 1

---

## 📊 Architecture Cible

```
Discord Users
     ↓
┌────────────────────────────────────────┐
│ SERVICE 3: Discord Bot (Spring Boot)   │
│ - Simplifié (handlers UI seulement)    │
│ - Appelle Service 2 via HTTP REST      │
└────┬─────────────────────────────────────┘
     │ HTTP: POST /api/seed/{mode}
     │       GET /api/presets/{mode}
     ↓
┌────────────────────────────────────────┐
│ SERVICE 2: Seed Generator (Spring Boot)│
│ - Orchestrateur central                │
│ - Gère: Franco, S8, S9, Allsanity,    │
│         Salad, ToT, Mixed              │
│ - Appelle Service 1 pour RSL/POT/ROT  │
│ - Appelle ootrandomizer.com API        │
└────┬─────────────────────────────────────┘
     │ HTTP: POST /api/generate/settings
     ↓
┌────────────────────────────────────────┐
│ SERVICE 1: RSL API (FastAPI, Python)   │
│ - Wraps plando-random-settings         │
│ - Génère RSL settings uniquement       │
│ - Stateless, deployable horizontalement│
└────────────────────────────────────────┘
```

---

## 📋 Fichiers Critiques

### Service 1 (RSL API - à créer)
```
rsl-api-service/
├── requirements.txt                  # Python 3.12+ deps
├── Dockerfile                        # Multi-stage uvicorn
├── docker-compose.yml               # Local dev
├── src/
│   ├── main.py                      # FastAPI app
│   ├── routes/
│   │   └── generate.py              # POST /api/generate/settings
│   ├── services/
│   │   └── rsl_generator.py         # Orchestrates RandomSettingsGenerator.py
│   └── models/
│       └── schemas.py               # Pydantic request/response models
└── plando-random-settings/          # (copié du repo existant)
    ├── RandomSettingsGenerator.py
    ├── roll_settings.py
    ├── validation.py
    ├── conditionals.py
    ├── weights/                     # All JSON weight files
    ├── randomizer/                  # OoT randomizer (90+ files)
    └── data/                        # EPHEMERAL VOL
```

### Service 2 (Seed Generator - à créer)
```
seed-generator-service/
├── src/main/java/fr/rawz06/seeding/
│   ├── api/
│   │   ├── rsl/                     # NEW
│   │   │   ├── RslApiClient.java    # KEY: HTTP client vers Service 1
│   │   │   ├── RslApiConfig.java
│   │   │   └── RslApiException.java
│   │   └── randomizer/              # De: bot
│   ├── web/
│   │   └── SeedController.java      # NEW: REST endpoints
│   ├── usecases/                    # Copié du bot
│   │   ├── franco/
│   │   ├── rsl/                     # ← Modifié pour appeler RslApiClient
│   │   ├── s8/
│   │   ├── s9/
│   │   ├── allsanity/
│   │   ├── salad/
│   │   ├── tot/
│   │   └── mixed/
│   ├── entities/                    # Shared domain models
│   └── services/
│       └── SeedService.java         # Refactorisé
├── pom.xml                          # Dépendances réduites
└── application-prod.properties      # Config
```

### Service 3 (Discord Bot - à modifier)
```
discord-bot-service/
├── src/main/java/fr/rawz06/rslfrancobot/
│   ├── external/
│   │   └── SeedGeneratorClient.java # NEW: HTTP client vers Service 2
│   ├── api/discord/                 # GARDER
│   │   ├── JDAEventListener.java
│   │   ├── JDASlashCommandAdapter.java
│   │   └── JDAInteractionAdapter.java
│   ├── bot/                         # GARDER
│   │   ├── presenters/
│   │   └── services/                # Simplifié
│   ├── api/python/                  # ❌ À SUPPRIMER
│   │   └── PythonRSLScriptAdapter.java
│   ├── engine/                      # ❌ À SUPPRIMER (copié en Service 2)
│   └── web/
│       └── SeedRestController.java  # À réévaluer
├── pom.xml                          # Épuré
└── application-prod.properties      # Config
```

---

## 🗓️ Plan d'Implémentation (3-4 semaines)

### **PHASE 1: Service 1 (RSL API) - 2-3 jours**
**Risque:** ✅ Très bas (totalement isolé, pas de rupture avec bot existant)

- [ ] Créer repo `rsl-api-service` en Git
- [ ] Créer structure FastAPI basique
  - [ ] `src/main.py` (FastAPI app)
  - [ ] `src/routes/generate.py` (POST /api/generate/settings)
  - [ ] `src/services/rsl_generator.py` (wrapper)
- [ ] Copier `plando-random-settings/` complet
- [ ] Dockerfile multi-stage
- [ ] Tester localement avec `/api/generate/settings`
- [ ] Déployer Service 1 (prod ou staging)
- [ ] **Payload attendu:**
  ```json
  POST /api/generate/settings
  {
    "mode": "rsl",
    "weights_file": "weights/rsl_main.json",
    "override": null
  }
  ```
- [ ] **Response:**
  ```json
  {
    "success": true,
    "settings": {...},
    "seed_url": "https://...",
    "timestamp": "2026-09-14T..."
  }
  ```

**Blockers:** ROM file 64MB (solution: VOL Docker secret)

---

### **PHASE 2: Service 2 (Seed Generator) - 3-4 jours**
**Risque:** ⚠️ Bas (Spring Boot, code métier existant réutilisé)

- [ ] Créer repo `seed-generator-service`
- [ ] Copier usecases du bot (`engine/usecases/`)
- [ ] **KEY FILE:** Créer `RslApiClient.java`
  ```java
  @Component
  public class RslApiClient {
      @Value("${app.rsl.api.base-url}")
      private String rslApiUrl;
      
      public SettingsFile generateSettings(String mode, String weightOverride) {
          // POST to Service 1: /api/generate/settings
          // Return SettingsFile
      }
  }
  ```
- [ ] Modifier `GenerateRSLSeedUseCase.java`
  - [ ] Remplacer `PythonRSLScriptAdapter` par `RslApiClient`
- [ ] Créer `SeedController.java` (REST endpoints)
  - [ ] POST `/api/seed/{mode}`
  - [ ] GET `/api/presets/{mode}`
- [ ] Config: `application-prod.properties`
  ```properties
  app.rsl.api.base-url=http://rsl-api-service:8080
  app.rsl.api.timeout=30000
  app.rsl.api.retries=3
  ```
- [ ] Tester chaque mode (Franco, RSL, S8, S9, etc.)
- [ ] Feature flag pour basculer (fallback vers Python local si besoin)
  ```properties
  app.rsl.api.enabled=true
  app.rsl.api.fallback-to-local=true
  ```

**Blockers:** Randomizer API down (solution: circuit breaker, caching 5min)

---

### **PHASE 3: Service 3 (Discord Bot) - 2 jours**
**Risque:** ⚠️ Moyen (redirection UI, nettoyage code)

- [ ] Créer repo `discord-bot-service`
- [ ] **KEY FILE:** Créer `SeedGeneratorClient.java`
  ```java
  @Component
  public class SeedGeneratorClient {
      @Value("${app.seed-generator.api.base-url}")
      private String seedGenUrl;
      
      public SeedResult generateSeed(SeedMode mode, String userId, 
                                      Map<String, String> options) {
          // POST to Service 2: /api/seed/{mode}
      }
      
      public List<Preset> getPresets(String mode) {
          // GET from Service 2: /api/presets/{mode}
      }
  }
  ```
- [ ] Modifier handlers Discord
  ```java
  // BEFORE: SeedService.generateSeed(...)
  // AFTER: SeedGeneratorClient.generateSeed(...)
  ```
- [ ] **SUPPRIMER:**
  - [ ] `/src/main/java/fr/rawz06/rslfrancobot/api/python/` (PythonRSLScriptAdapter)
  - [ ] `/src/main/java/fr/rawz06/rslfrancobot/engine/` (logique métier → Service 2)
  - [ ] `/plando-random-settings/` (dossier entier)
- [ ] Config: `application-prod.properties`
  ```properties
  app.seed-generator.api.base-url=http://seed-gen-service:8081
  app.seed-generator.api.timeout=60000
  ```
- [ ] Tester flow Discord complet (slash command → Service 2 → Service 1)
- [ ] Déploiement canary: 10% trafic → 50% → 100%

**Blockers:** Service 2 down (solution: circuit breaker, fallback mode)

---

### **PHASE 4: Optionnel (Cleanup) - 1 jour**
- [ ] Retirer dépendances Python du bot
- [ ] Activer feature flag `app.rsl.api.enabled=false` pour test fallback
- [ ] Monitoring & alertes (latency, error rate)
- [ ] Documentation mise à jour
- [ ] Runbook incident créé

---

## 🎯 Dépendances Cachées Identifiées

| Dépendance | Lieu | Impact | Mitigation |
|-----------|------|--------|-----------|
| **ROM file (64MB)** | plando-random-settings/ | ⚠️ CRITIQUE | VOL Docker secret, S3 artifact |
| **Randomizer intégré** | 90+ fichiers Python | HAUTE | Reste dans Service 1 (trop lourd) |
| **PythonRSLScriptAdapter** | 1 classe Java | MOYENNE | Remplacer par RslApiClient (1 jour) |
| **weights/*.json** | 50+ fichiers | BASSE | Copier vers Service 1, versioner en git |
| **Python version** | update_randomizer.py | BASSE | Service 1 isolé, flexible |
| **ootrandomizer.com API** | Externe | MOYENNE | Utilisé par Service 1 & 2, circuit breaker |
| **Presets YAML** | Bot charge en mémoire | MOYENNE | Service 2 expose via API, bot consomme HTTP |

---

## ⚠️ Risques et Mitigation

| Risque | Sévérité | Mitigation |
|--------|----------|-----------|
| **ROM file manquant en prod** | 🔴 CRITIQUE | S3 artifact + téléchargement docker.build; fallback override local |
| **Service 1 crash (Randomizer)** | 🟠 HAUTE | Service 2 circuit breaker + fallback cache 5min |
| **Latency réseau** | 🟡 MOYENNE | Caching HTTP 5min; connection pooling; timeout 30s Service 1, 60s Service 2 |
| **Data consistency** | 🟡 MOYENNE | Service 2 = source-of-truth presets; Service 1 stateless |
| **Network partition** | 🟡 MOYENNE | Feature flag fallback: `app.rsl.api.enabled=false` → Python local |
| **Upgrade Python futurs** | 🟢 BASSE | Service 1 isolé; Python 3.14+ indépendant du bot |

---

## 🚀 Stratégie de Déploiement

### **Rollback en 5 min:**
```properties
# application-prod.properties
app.rsl.api.enabled=false  # Bot revient à Python local
# Redeploy bot v2.x si critique
```

### **Feature flags (Spring Cloud Config):**
```properties
app.rsl.api.enabled=true                # Activer Service 1
app.rsl.api.base-url=http://...         # URL Service 1
app.seed-generator.api.enabled=true     # Activer Service 2
app.seed-generator.api.base-url=http://... # URL Service 2
```

### **Déploiement bot (Phase 3):**
1. Feature flag OFF par défaut
2. Canary: 10% trafic → 50% → 100%
3. Monitoring: latency, error rate, seed generation success
4. Basculer feature flag ON en prod progressivement

---

## 📝 Checklist Pré-Implémentation

### Infrastructure
- [ ] Git repos créés (rsl-api-service, seed-generator-service, discord-bot-service)
- [ ] CI/CD pipelines configurés (GitHub Actions ou similaire)
- [ ] Docker registries accessibles
- [ ] Monitoring centralisé (logs, métriques, traces)
- [ ] Feature flag infrastructure (Spring Cloud Config ou Consul)

### Service 1
- [ ] FastAPI app fonctionne localement
- [ ] ROM file mounté en VOL Docker
- [ ] Tests d'intégration plando-random-settings
- [ ] Docker image build réussit
- [ ] Healthcheck endpoint

### Service 2
- [ ] RslApiClient compilé & testé
- [ ] GenerateRSLSeedUseCase modifié
- [ ] Tous les usecases copié + testé
- [ ] Spring Boot tests verts
- [ ] Healthcheck endpoint

### Service 3
- [ ] SeedGeneratorClient compilé & testé
- [ ] Handlers Discord modifiés
- [ ] Tests Discord manuels OK
- [ ] Feature flag OFF par défaut
- [ ] Healthcheck endpoint

### Post-Déploiement
- [ ] Monitoring alertes configurées
- [ ] Documentation mise à jour (API, architecture)
- [ ] Runbook incident créé
- [ ] Load test simulé
- [ ] Chaos test: crash Service 1 → vérifier fallback OK

---

## 📞 Points de Contact / Prochaines Étapes

**Décisions en attente:**
- [ ] Hosting des APIs (même serveur bot? séparé? k8s?)
- [ ] Authentification des APIs (publique? token? IP whitelist?)
- [ ] Sites web pour Services 1 & 2 (tech stack? React? Vanilla?)

**Démarrage Phase 1:**
1. Créer repo Git `rsl-api-service`
2. Créer structure FastAPI skeleton
3. Copier `plando-random-settings/`
4. Wrapper RandomSettingsGenerator en FastAPI
5. Tester POST /api/generate/settings

**Timeline suggérée:** Semaine 1 = Phase 1 en parallèle (0 rupture bot). Semaine 2-3 = Phases 2-3 si Phase 1 validée.

---

**Généré par Claude Haiku 4.5**  
Session: https://claude.ai/code/session_01UuW6YLzsjGNhx6kRzmy6eb
