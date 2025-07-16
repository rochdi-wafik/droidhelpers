# ChangeLog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 12-08-2024
### Added
- Add new method to AddressHelper{} which is isValidCidr()
* This method check if the given param is valid CIDR, like: 10.0.0.1/32

## 25-08-2024
### Added
* Add SqlPreferences API
* Add AES Encryption Decryption 

## 16-07-2025
### Added
* Add new helpers, like HttpClient
* 
### Changed
* Support Android-15
* Update for loops: fix indexOutOfBound because we remove from list during loop without update i value using i--