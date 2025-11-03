# Analysis, Modeling and Design of Personalized Digital Learning Environment: A Proof of Concept

This research analyzes, models and develops a novel Android-based Digital Learning Environment (DLE) powered by a Private Learning Intelligence (PLI) framework. 

The PLI layer leverages **on-device TensorFlow Lite** and **federated-style hooks** to train and refine personalized learning models while keeping user data local. 

The approach enables real-time, private personalization across login/usage telemetry and quiz interactions, and reduces the instructional overhead to deliver individualized learning. 

The prototype demonstrates how **local personal models** and **global knowledge models** can coexist—personal models learn from each learner’s data on device, while global models provide generalized knowledge for cold start and consistency. 


## Proposed Architecture
![fig3](https://github.com/pli-research-d/Secure-ML-with-BERT/blob/069015f8bbc4271a644b3b94cbf3bf0e6ec7f3b3/PLI%20Architecture.png)

Within the PLI, two challenges are addressed: 

1) **Local training with privacy**—data never leaves the device; learning occurs via TFLite signatures and optional transfer‑learning updates. 

2) **Personal ↔ Global fusion**—the app can export weight deltas for optional aggregation (server‑side, out of scope here) to update a global model that later improves the base model shipped to devices. 


## Key Aspects

### Secure Environment (Android)
- **On‑device inference:** `TFLitePersonalizationManager` loads `assets/dle_model.tflite` and performs predictions locally. 
- **No network, by default:** The app ships without network calls to prevent unintended data exfiltration. 
- **User control:** Clear‑data action on the dashboard and local `SharedPreferences`/SQLite only. 

### Learning Dataset (Telemetry & Quiz)
- **Quiz interactions:** `QuizActivity` records category, correctness, timing, difficulty, explanations, and examples. 
- **Usage signals:** `UserStatsManager` tracks sessions and simple engagement stats for personalization loops. 
- **Local store:** `DBHelper` manages user credentials; lightweight stats remain in preferences. 

### Model Setup (TFLite)
- **Personal model path:** `assets/dle_model.tflite` (signature model optional). 
- **Wrapper classes:** `TFLitePersonalizationManager` (simple 1×5 → 1×4 inference) and `TransferLearningModelWrapper` (signature‑based `train`/`infer`/`export`). 

### Data Loading and Preparation
- **Inputs:** Five numeric inputs (demo) from `UserInputActivity` mapped to four outputs. 
- **Scaling:** Performed in `TFLitePersonalizationManager` before inference; adjust to your model. 

### Training and Validation (On‑device, optional)
- **One‑step training:** `OnDeviceFederatedLearningManager` calls `train` signature for a single update step. 
- **Diagnostics:** `ModelDiagnosticsActivity` and `ModelValidator` verify the model loads and signatures resolve. 

### Hyperparameter Tuning (Out‑of‑scope on device)
- **Recommendation:** Tune centrally (Python/Colab) and ship tuned base model; device performs light updates only. 

### Inference and Learning Measures
- **Predict:** `TFLitePersonalizationManager#predict(float[] input)` returns four normalized outputs. 
- **Interpretation:** Map outputs to scores like *Conscientiousness, Motivation, Understanding, Engagement* in the UI. 

### Model Averaging (Federated‑style)
- **Export weights:** `TransferLearningModelWrapper#exportWeights()` obtains a `ByteBuffer` of deltas. 
- **Server aggregation:** Aggregate off‑device (future work) and redistribute updated global model in app updates. 

### Contextual Chatbot (Optional future module)
- **NLP adapter:** Could use an on‑device or server LLM adapter to generate tips guided by learning scores and topics. 


## Implementation Details

1. **Project Structure (Core Java classes)**
    - `MainActivity`, `Registration`, `DBHelper` for auth. 
    - `Dashboard` for navigation, quick demo inference, clear‑data action. 
    - `QuizActivity`, `Question` for content and interactions. 
    - `UserInputActivity` for 5‑input prediction demo. 
    - `TFLitePersonalizationManager`, `TransferLearningModelWrapper`, `OnDeviceFederatedLearningManager` for ML. 
    - `ModelDiagnosticsActivity`, `ModelValidator` for runtime checks. 

2. **Assets**
    - `app/src/main/assets/dle_model.tflite` — base or personalized model file. 
    - `app/src/main/assets/questions.json` — quiz items with categories, difficulty, explanation, example. 

3. **Required Layouts & IDs (Checklist)**
    - `activity_main.xml` → `editTextUsername`, `editTextPassword`, `textViewRegister`, `buttonLogin`. 
    - `activity_registration.xml` → `usernameEditText`, `passwordEditText`, `registerButton`. 
    - `activity_dashboard.xml` → `welcomeText`, category buttons (`btnCategoryHTML`/`CSS`/`JS`/`PHP`/`MYSQL`/`PYTHON`), `btnQuiz`, `btnUserInput`, `btnDiagnostics`, `btnPersonalization`, `btnClearData`. 
    - `activity_quiz.xml` → `quizContentLayout`, `categoryButtonsLayout`, `questionText`, `questionCounter`, `explanationText`, `exampleText`, `metaText`, `buttonTrue`, `buttonFalse`, `buttonOptionA/B/C/D`, `buttonNext`. 
    - `activity_user_input.xml` → `etVal1..etVal5`, `tvPrediction`, `btnPredict`. 
    - `activity_personalization.xml` → `tvPersonOutput`, `btnRun`. 
    - `activity_model_diagnostics.xml` → `tvStatus`, `btnRunChecks`. 

4. **Gradle & SDK (Example)**
    ```gradle
    implementation "androidx.appcompat:appcompat:1.7.0"
    implementation "com.google.android.material:material:1.12.0"
    implementation "androidx.constraintlayout:constraintlayout:2.2.0"
    implementation "org.tensorflow:tensorflow-lite:2.14.0"
    // Optional:
    // implementation "org.tensorflow:tensorflow-lite-support:0.4.4"
    // implementation "org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0"
    ```
    - Min SDK 24; compile/target SDK 34 recommended. 
    - Enable ViewBinding or ensure all IDs exist. 

5. **Model Signatures (If using signature runner)**
    - Methods: `"train"`, `"infer"`, `"export"`. 
    - Ensure input/output keys in `TransferLearningModelWrapper` match your model (inspect with Netron). 

6. **Quiz JSON (Minimal Example)**
    ```json
    [
      {
        "category": "HTML",
        "type": "tf",
        "prompt": "The <div> element is a block element.",
        "answer": true,
        "difficulty": 0.3,
        "explanation": "div defaults to display:block.",
        "example": "<div>...</div>"
      },
      {
        "category": "CSS",
        "type": "mcq",
        "prompt": "Which selector targets an id?",
        "options": ["#", ".", "[]", "*"],
        "answerIndex": 0,
        "difficulty": 0.4,
        "explanation": "The # hash selects by id.",
        "example": "#main { color:red; }"
      }
    ]
    ```

7. **Privacy & Security**
    - On‑device by default; add network only with explicit consent and clear purpose. 
    - Do not include secrets; validate local storage needs; offer a clear‑data UX. 

8. **Troubleshooting**
    - **Model not found:** Ensure `assets/dle_model.tflite` exists and path matches. 
    - **Signature/key mismatch:** Verify names with Netron and update wrapper maps. 
    - **Missing view IDs:** Cross‑check the checklist. 
    - **Weird predictions:** Check input scaling/order and output mapping. 

9. **Roadmap (Suggested)**
    - Adaptive quiz difficulty and mastery tracking. 
    - Background `WorkManager` to export deltas when charging on Wi‑Fi. 
    - Global model aggregation pipeline and over‑the‑air model updates. 
    - Optional on‑device NLP coach powered by scores/topics. 


## Video
**Screen recording of the process (placeholder).** 

Add a link or GIF once available. 


## Getting Started

1. **Clone & open** in Android Studio (Giraffe+). 
2. **Add assets**: `dle_model.tflite` and `questions.json`. 
3. **Build & run** on ARM64 device/emulator. 
4. **Try flows**: register → login → dashboard → quiz/user input → diagnostics. 


## Further Information

- TensorFlow Lite guide — https://www.tensorflow.org/lite/guide 
- Netron (inspect model signatures) — https://netron.app/ 
- Android ML docs — https://developer.android.com/jetpack/androidx/releases/ml 
