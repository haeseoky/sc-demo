#!/bin/bash

# 🚀 K6 캐시 성능 테스트 실행 스크립트

set -e

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Spring Boot 애플리케이션 상태 확인
check_application() {
    log_info "Spring Boot 애플리케이션 상태 확인 중..."
    
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/cache/users/test | grep -q "200"; then
            log_success "애플리케이션이 정상 실행 중입니다."
            return 0
        fi
        
        log_warning "애플리케이션 연결 시도 $attempt/$max_attempts..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "애플리케이션에 연결할 수 없습니다. 8080 포트에서 실행 중인지 확인하세요."
    exit 1
}

# 캐시 예열
warmup_cache() {
    log_info "캐시 예열 중..."
    
    local response=$(curl -s -X POST http://localhost:8080/api/cache/warmup)
    if [[ $? -eq 0 ]]; then
        log_success "캐시 예열 완료: $response"
        sleep 5  # 예열 완료 대기
    else
        log_warning "캐시 예열에 실패했지만 테스트를 계속 진행합니다."
    fi
}

# Docker 상태 확인
check_docker() {
    log_info "Docker 상태 확인 중..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker가 설치되어 있지 않습니다."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker가 실행 중이지 않습니다."
        exit 1
    fi
    
    log_success "Docker가 정상 실행 중입니다."
}

# 결과 디렉토리 준비
prepare_results_dir() {
    log_info "결과 디렉토리 준비 중..."
    
    # 타임스탬프로 결과 디렉토리 생성
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    export RESULTS_DIR="results/test_${timestamp}"
    
    mkdir -p "$RESULTS_DIR"
    log_success "결과 저장 경로: $RESULTS_DIR"
}

# 테스트 실행 함수
run_performance_test() {
    log_info "📊 종합 성능 테스트 시작..."
    
    docker run --rm \
        --network host \
        -v $(pwd)/k6:/scripts \
        -v $(pwd)/${RESULTS_DIR}:/results \
        -e BASE_URL=http://localhost:8080 \
        grafana/k6:latest run \
        --out json=/results/performance-test.json \
        --summary-export=/results/performance-summary.json \
        /scripts/cache-performance-test.js
    
    if [[ $? -eq 0 ]]; then
        log_success "성능 테스트 완료"
    else
        log_error "성능 테스트 실패"
        return 1
    fi
}

run_stress_test() {
    log_info "🔥 스트레스 테스트 시작..."
    
    docker run --rm \
        --network host \
        -v $(pwd)/k6:/scripts \
        -v $(pwd)/${RESULTS_DIR}:/results \
        -e BASE_URL=http://localhost:8080 \
        grafana/k6:latest run \
        --out json=/results/stress-test.json \
        --summary-export=/results/stress-summary.json \
        /scripts/cache-stress-test.js
    
    if [[ $? -eq 0 ]]; then
        log_success "스트레스 테스트 완료"
    else
        log_error "스트레스 테스트 실패"
        return 1
    fi
}

run_spike_test() {
    log_info "⚡ 스파이크 테스트 시작..."
    
    docker run --rm \
        --network host \
        -v $(pwd)/k6:/scripts \
        -v $(pwd)/${RESULTS_DIR}:/results \
        -e BASE_URL=http://localhost:8080 \
        grafana/k6:latest run \
        --out json=/results/spike-test.json \
        --summary-export=/results/spike-summary.json \
        /scripts/cache-spike-test.js
    
    if [[ $? -eq 0 ]]; then
        log_success "스파이크 테스트 완료"
    else
        log_error "스파이크 테스트 실패"
        return 1
    fi
}

# 테스트 결과 요약
summarize_results() {
    log_info "📊 테스트 결과 요약 생성 중..."
    
    local summary_file="${RESULTS_DIR}/test-summary.txt"
    
    {
        echo "🚀 K6 캐시 성능 테스트 결과 요약"
        echo "================================="
        echo "실행 시간: $(date)"
        echo "결과 경로: $RESULTS_DIR"
        echo ""
        
        # 각 테스트 결과 파일이 존재하면 요약 정보 추출
        if [[ -f "${RESULTS_DIR}/performance-summary.json" ]]; then
            echo "📊 성능 테스트 결과:"
            jq -r '.metrics | to_entries[] | select(.key | contains("http_req")) | "  \(.key): \(.value.avg // .value.rate // .value.count)"' "${RESULTS_DIR}/performance-summary.json" 2>/dev/null || echo "  결과 파싱 실패"
            echo ""
        fi
        
        if [[ -f "${RESULTS_DIR}/stress-summary.json" ]]; then
            echo "🔥 스트레스 테스트 결과:"
            jq -r '.metrics | to_entries[] | select(.key | contains("http_req")) | "  \(.key): \(.value.avg // .value.rate // .value.count)"' "${RESULTS_DIR}/stress-summary.json" 2>/dev/null || echo "  결과 파싱 실패"
            echo ""
        fi
        
        if [[ -f "${RESULTS_DIR}/spike-summary.json" ]]; then
            echo "⚡ 스파이크 테스트 결과:"
            jq -r '.metrics | to_entries[] | select(.key | contains("http_req")) | "  \(.key): \(.value.avg // .value.rate // .value.count)"' "${RESULTS_DIR}/spike-summary.json" 2>/dev/null || echo "  결과 파싱 실패"
            echo ""
        fi
        
        echo "상세 결과는 JSON 파일을 확인하세요:"
        ls -la "${RESULTS_DIR}/"*.json 2>/dev/null || echo "  JSON 결과 파일 없음"
        
    } > "$summary_file"
    
    # 콘솔에도 출력
    cat "$summary_file"
    
    log_success "결과 요약 저장: $summary_file"
}

# 사용법 표시
show_usage() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  -p, --performance    성능 테스트만 실행"
    echo "  -s, --stress        스트레스 테스트만 실행"
    echo "  -k, --spike         스파이크 테스트만 실행"
    echo "  -a, --all           모든 테스트 실행 (기본값)"
    echo "  -h, --help          도움말 표시"
    echo ""
    echo "예제:"
    echo "  $0                  # 모든 테스트 실행"
    echo "  $0 -p               # 성능 테스트만 실행"
    echo "  $0 --stress         # 스트레스 테스트만 실행"
}

# 메인 실행 로직
main() {
    local run_performance=false
    local run_stress=false
    local run_spike=false
    local run_all=true
    
    # 명령행 인자 처리
    while [[ $# -gt 0 ]]; do
        case $1 in
            -p|--performance)
                run_performance=true
                run_all=false
                shift
                ;;
            -s|--stress)
                run_stress=true
                run_all=false
                shift
                ;;
            -k|--spike)
                run_spike=true
                run_all=false
                shift
                ;;
            -a|--all)
                run_all=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                log_error "알 수 없는 옵션: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # 모든 테스트 실행이 기본값
    if [[ "$run_all" == true ]]; then
        run_performance=true
        run_stress=true
        run_spike=true
    fi
    
    log_info "🚀 K6 캐시 성능 테스트 시작"
    
    # 사전 체크
    check_docker
    check_application
    prepare_results_dir
    warmup_cache
    
    local test_count=0
    local failed_count=0
    
    # 테스트 실행
    if [[ "$run_performance" == true ]]; then
        test_count=$((test_count + 1))
        if ! run_performance_test; then
            failed_count=$((failed_count + 1))
        fi
        sleep 5  # 테스트 간 대기
    fi
    
    if [[ "$run_stress" == true ]]; then
        test_count=$((test_count + 1))
        if ! run_stress_test; then
            failed_count=$((failed_count + 1))
        fi
        sleep 5  # 테스트 간 대기
    fi
    
    if [[ "$run_spike" == true ]]; then
        test_count=$((test_count + 1))
        if ! run_spike_test; then
            failed_count=$((failed_count + 1))
        fi
    fi
    
    # 결과 요약
    summarize_results
    
    # 최종 결과
    local success_count=$((test_count - failed_count))
    
    echo ""
    log_info "🏁 테스트 완료!"
    log_info "총 테스트: $test_count"
    log_success "성공: $success_count"
    
    if [[ $failed_count -gt 0 ]]; then
        log_error "실패: $failed_count"
        exit 1
    else
        log_success "모든 테스트가 성공적으로 완료되었습니다! 🎉"
        exit 0
    fi
}

# 스크립트 실행
main "$@"