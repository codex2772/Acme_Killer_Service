# JewelERP — Customer Enquiry Image Upload (Flutter Integration Guide)

> **Last Updated:** 2026-03-19

---

## Overview

Customers can optionally attach an image when submitting an enquiry via the Flutter mobile app. Images are uploaded to **AWS S3** (`enquiries/` folder) and the public URL is saved with the enquiry.

**Flow:**
```
1. Customer picks image (gallery/camera)
2. App uploads image  →  POST /api/customer-app/images/upload
3. Backend returns S3 URL
4. App submits enquiry with imageUrl  →  POST /api/customer-app/enquiry
5. Backend saves enquiry + imageUrl to DB
```

---

## API Endpoints

### 1. Upload Image

```
POST /api/customer-app/images/upload
```

| Header | Value |
|--------|-------|
| `Authorization` | `Bearer <customer_jwt_token>` |
| `Content-Type` | `multipart/form-data` (auto-set by client) |

| Form Field | Type | Required | Description |
|------------|------|----------|-------------|
| `file` | **File** | ✅ | Image file (max 5MB) |

**Allowed types:** `image/jpeg` · `image/png` · `image/webp` · `image/gif`

**Success — `200 OK`:**
```json
{
  "imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/844732ce-205f-4658-918a-50dbda859cea.png"
}
```

**Errors — `400 Bad Request`:**
```json
{"error": "File is empty"}
{"error": "File size exceeds 5MB limit"}
{"error": "Only JPEG, PNG, WebP, and GIF images are allowed"}
```

**Error — `401 Unauthorized`:**
```json
{"error": "Unauthorized", "message": "Authentication required"}
```

---

### 2. Submit Enquiry (with optional image)

```
POST /api/customer-app/enquiry
```

| Header | Value |
|--------|-------|
| `Authorization` | `Bearer <customer_jwt_token>` |
| `Content-Type` | `application/json` |

**Request Body:**
```json
{
  "jewelryItemId": 2,
  "subject": "Available in size 7?",
  "message": "Hi, is this ring available in size 7? Attaching a reference image.",
  "imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/844732ce-205f-4658-918a-50dbda859cea.png"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `jewelryItemId` | Long | ❌ | Jewelry item ID (null for general enquiry) |
| `subject` | String | ✅ | Enquiry subject line |
| `message` | String | ✅ | Enquiry message body |
| `imageUrl` | String | ❌ | S3 URL from upload step (null if no image) |

**Success — `201 Created`:**
```json
{
  "id": 5,
  "jewelryItemId": 2,
  "jewelryItemName": "Diamond Solitaire Ring",
  "subject": "Available in size 7?",
  "message": "Hi, is this ring available in size 7? Attaching a reference image.",
  "imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/844732ce-205f-4658-918a-50dbda859cea.png",
  "status": "OPEN",
  "adminResponse": null,
  "createdAt": "2026-03-19T10:30:00Z",
  "respondedAt": null
}
```

---

### 3. Get My Enquiries (includes imageUrl)

```
GET /api/customer-app/enquiries
```

| Header | Value |
|--------|-------|
| `Authorization` | `Bearer <customer_jwt_token>` |

**Success — `200 OK`:**
```json
[
  {
    "id": 5,
    "jewelryItemId": 2,
    "jewelryItemName": "Diamond Solitaire Ring",
    "subject": "Available in size 7?",
    "message": "Hi, is this ring available in size 7?",
    "imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/844732ce.png",
    "status": "RESPONDED",
    "adminResponse": "Yes, size 7 is available. Visit us this weekend!",
    "createdAt": "2026-03-19T10:30:00Z",
    "respondedAt": "2026-03-19T12:00:00Z"
  },
  {
    "id": 3,
    "jewelryItemId": null,
    "jewelryItemName": null,
    "subject": "Custom order enquiry",
    "message": "I want a custom necklace design.",
    "imageUrl": null,
    "status": "OPEN",
    "adminResponse": null,
    "createdAt": "2026-03-18T08:00:00Z",
    "respondedAt": null
  }
]
```

> `imageUrl` is `null` when no image was attached to the enquiry.

---

## Flutter Code

### Dependencies

```yaml
# pubspec.yaml
dependencies:
  http: ^1.2.0
  image_picker: ^1.0.7
```

### 1. Pick Image

```dart
import 'dart:io';
import 'package:image_picker/image_picker.dart';

Future<File?> pickImage({ImageSource source = ImageSource.gallery}) async {
  final picker = ImagePicker();
  final picked = await picker.pickImage(
    source: source,
    maxWidth: 1024,
    maxHeight: 1024,
    imageQuality: 80, // compress to 80%
  );
  return picked != null ? File(picked.path) : null;
}
```

### 2. Upload Image to S3

```dart
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

const String baseUrl = 'http://jewel-erp-alb-xxx.ap-south-1.elb.amazonaws.com';

Future<String?> uploadEnquiryImage(File imageFile, String token) async {
  final uri = Uri.parse('$baseUrl/api/customer-app/images/upload');

  final request = http.MultipartRequest('POST', uri)
    ..headers['Authorization'] = 'Bearer $token'
    ..files.add(await http.MultipartFile.fromPath('file', imageFile.path));

  final response = await request.send();

  if (response.statusCode == 200) {
    final body = jsonDecode(await response.stream.bytesToString());
    return body['imageUrl'] as String;
  } else {
    final body = jsonDecode(await response.stream.bytesToString());
    throw Exception(body['error'] ?? 'Image upload failed');
  }
}
```

### 3. Submit Enquiry with Image

```dart
Future<Map<String, dynamic>> submitEnquiry({
  required String token,
  required String subject,
  required String message,
  int? jewelryItemId,
  File? imageFile,
}) async {
  // Step 1: Upload image if provided
  String? imageUrl;
  if (imageFile != null) {
    imageUrl = await uploadEnquiryImage(imageFile, token);
  }

  // Step 2: Submit enquiry with imageUrl
  final response = await http.post(
    Uri.parse('$baseUrl/api/customer-app/enquiry'),
    headers: {
      'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
    },
    body: jsonEncode({
      if (jewelryItemId != null) 'jewelryItemId': jewelryItemId,
      'subject': subject,
      'message': message,
      if (imageUrl != null) 'imageUrl': imageUrl,
    }),
  );

  if (response.statusCode == 201) {
    return jsonDecode(response.body);
  } else {
    throw Exception('Failed to submit enquiry: ${response.body}');
  }
}
```

### 4. Display Enquiry Image

```dart
Widget buildEnquiryImage(String? imageUrl) {
  if (imageUrl == null || imageUrl.isEmpty) {
    return const SizedBox.shrink(); // No image — hide widget
  }
  return ClipRRect(
    borderRadius: BorderRadius.circular(8),
    child: Image.network(
      imageUrl,
      width: double.infinity,
      height: 200,
      fit: BoxFit.cover,
      loadingBuilder: (context, child, progress) {
        if (progress == null) return child;
        return const SizedBox(
          height: 200,
          child: Center(child: CircularProgressIndicator()),
        );
      },
      errorBuilder: (_, __, ___) => Container(
        height: 200,
        color: Colors.grey[200],
        child: const Center(child: Icon(Icons.broken_image, size: 50)),
      ),
    ),
  );
}
```

### 5. Complete Enquiry Form Widget

```dart
class EnquiryFormScreen extends StatefulWidget {
  final String token;
  final int? jewelryItemId;
  final String? jewelryItemName;

  const EnquiryFormScreen({
    required this.token,
    this.jewelryItemId,
    this.jewelryItemName,
    super.key,
  });

  @override
  State<EnquiryFormScreen> createState() => _EnquiryFormScreenState();
}

class _EnquiryFormScreenState extends State<EnquiryFormScreen> {
  final _subjectController = TextEditingController();
  final _messageController = TextEditingController();
  File? _selectedImage;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    if (widget.jewelryItemName != null) {
      _subjectController.text = 'Enquiry about ${widget.jewelryItemName}';
    }
  }

  Future<void> _pickImage() async {
    // Show bottom sheet to choose camera or gallery
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (_) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: const Text('Camera'),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Gallery'),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
          ],
        ),
      ),
    );

    if (source != null) {
      final image = await pickImage(source: source);
      if (image != null) {
        setState(() => _selectedImage = image);
      }
    }
  }

  void _removeImage() {
    setState(() => _selectedImage = null);
  }

  Future<void> _submit() async {
    final subject = _subjectController.text.trim();
    final message = _messageController.text.trim();

    if (subject.isEmpty || message.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Subject and message are required')),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    try {
      await submitEnquiry(
        token: widget.token,
        subject: subject,
        message: message,
        jewelryItemId: widget.jewelryItemId,
        imageFile: _selectedImage,
      );

      if (mounted) {
        Navigator.pop(context, true); // return success
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Enquiry submitted successfully!'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: ${e.toString()}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('New Enquiry')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Subject
            TextField(
              controller: _subjectController,
              decoration: const InputDecoration(
                labelText: 'Subject *',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),

            // Message
            TextField(
              controller: _messageController,
              decoration: const InputDecoration(
                labelText: 'Message *',
                border: OutlineInputBorder(),
                alignLabelWithHint: true,
              ),
              maxLines: 5,
            ),
            const SizedBox(height: 16),

            // Image attachment
            if (_selectedImage != null) ...[
              Stack(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.file(
                      _selectedImage!,
                      height: 200,
                      width: double.infinity,
                      fit: BoxFit.cover,
                    ),
                  ),
                  Positioned(
                    top: 8,
                    right: 8,
                    child: GestureDetector(
                      onTap: _removeImage,
                      child: Container(
                        padding: const EdgeInsets.all(4),
                        decoration: const BoxDecoration(
                          color: Colors.red,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.close,
                          color: Colors.white,
                          size: 20,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'Image attached',
                style: TextStyle(color: Colors.grey[600], fontSize: 12),
              ),
            ] else
              OutlinedButton.icon(
                onPressed: _pickImage,
                icon: const Icon(Icons.attach_file),
                label: const Text('Attach Reference Image (optional)'),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),

            const SizedBox(height: 24),

            // Submit button
            ElevatedButton(
              onPressed: _isSubmitting ? null : _submit,
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: _isSubmitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Submit Enquiry', style: TextStyle(fontSize: 16)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _subjectController.dispose();
    _messageController.dispose();
    super.dispose();
  }
}
```

---

## Flow Diagram

```
┌──────────────────┐
│  Customer opens   │
│  Enquiry Form     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐     ┌─────────────────────┐
│ Pick image       │────►│ image_picker plugin  │
│ (optional)       │◄────│ camera / gallery     │
└────────┬─────────┘     └─────────────────────┘
         │
         ▼ (if image selected)
┌──────────────────────────────────────┐
│ POST /api/customer-app/images/upload │
│ Body: multipart/form-data            │
│ Response: {"imageUrl": "https://…"}  │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ POST /api/customer-app/enquiry       │
│ Body: {                              │
│   "subject": "...",                  │
│   "message": "...",                  │
│   "jewelryItemId": 2,               │
│   "imageUrl": "https://…" or null   │
│ }                                    │
│ Response: 201 Created                │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ GET /api/customer-app/enquiries      │
│ Response: [{                         │
│   "id": 5,                           │
│   "subject": "...",                  │
│   "imageUrl": "https://…",          │
│   "status": "OPEN",                 │
│   "adminResponse": null             │
│ }]                                   │
└──────────────────────────────────────┘
```

---

## Constraints

| Constraint | Value |
|------------|-------|
| Max file size | **5 MB** |
| Allowed types | JPEG, PNG, WebP, GIF |
| S3 folder | `enquiries/` |
| S3 bucket | `jewel-erp-images-ap-south-1` |
| Image URL format | `https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/<uuid>.<ext>` |
| Auth required | ✅ Customer JWT token |
| Images per enquiry | 1 (single `imageUrl` field) |
